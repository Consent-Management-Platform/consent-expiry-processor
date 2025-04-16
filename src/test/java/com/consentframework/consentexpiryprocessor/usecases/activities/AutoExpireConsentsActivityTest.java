package com.consentframework.consentexpiryprocessor.usecases.activities;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.consentframework.consentexpiryprocessor.domain.entities.ActiveConsentWithExpiryTime;
import com.consentframework.consentexpiryprocessor.domain.repositories.ConsentRepository;
import com.consentframework.consentexpiryprocessor.infrastructure.repositories.InMemoryConsentRepository;
import com.consentframework.consentexpiryprocessor.testcommon.utils.ActiveConsentWithExpiryTimeGenerator;
import com.consentframework.shared.api.domain.pagination.ListPage;
import com.consentframework.shared.api.infrastructure.mappers.DynamoDbConsentExpiryTimeConverter;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class AutoExpireConsentsActivityTest {
    @Test
    void executeWhenNoConsents() {
        final InMemoryConsentRepository repository = spy(new InMemoryConsentRepository());
        new AutoExpireConsentsActivity(repository).execute();

        validateNoConsentsExpired(repository);
    }

    @Test
    void executeWhenNullPageReturned() {
        final InMemoryConsentRepository repository = mock(InMemoryConsentRepository.class);
        final Optional<String> expectedGetActiveConsentsInput = Optional.empty();
        doReturn(null).when(repository).getActiveConsentsWithExpiryHour(anyString(), eq(expectedGetActiveConsentsInput));

        new AutoExpireConsentsActivity(repository).execute();

        validateNoConsentsExpired(repository);
    }

    @Test
    void executeWhenNullResultsOnPage() {
        final InMemoryConsentRepository repository = mock(InMemoryConsentRepository.class);
        final Optional<String> expectedGetActiveConsentsInput = Optional.empty();
        final ListPage<ActiveConsentWithExpiryTime> mockPageConsents = new ListPage<>(null, Optional.empty());
        doReturn(mockPageConsents).when(repository).getActiveConsentsWithExpiryHour(anyString(), eq(expectedGetActiveConsentsInput));

        new AutoExpireConsentsActivity(repository).execute();

        validateNoConsentsExpired(repository);
    }

    @Test
    void executeWhenMultiplePageResults() {
        final OffsetDateTime firstHourDatetime = OffsetDateTime.now().minusHours(2).truncatedTo(ChronoUnit.HOURS);
        final String firstExpiryHour = DynamoDbConsentExpiryTimeConverter.toExpiryHour(firstHourDatetime);
        final String currentExpiryHour = DynamoDbConsentExpiryTimeConverter.toExpiryHour(firstHourDatetime.plusHours(2));

        final List<ActiveConsentWithExpiryTime> consentsExpiringFirstHour = List.of(
            ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), firstHourDatetime.plusMinutes(1)),
            ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), firstHourDatetime.plusMinutes(10)),
            ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), firstHourDatetime.plusMinutes(20)),
            ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), firstHourDatetime.plusMinutes(30)),
            ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), firstHourDatetime.plusMinutes(40))
        );
        final List<ActiveConsentWithExpiryTime> consentsExpiringCurrentHour = List.of(
            ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), OffsetDateTime.now().minusSeconds(1)),
            ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), OffsetDateTime.now().plusMinutes(5)),
            ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), OffsetDateTime.now().plusMinutes(15))
        );
        final Map<String, List<ActiveConsentWithExpiryTime>> consentsExpiringPerHour = Map.of(
            firstExpiryHour, consentsExpiringFirstHour,
            currentExpiryHour, consentsExpiringCurrentHour);
        final InMemoryConsentRepository repository = spy(new InMemoryConsentRepository(consentsExpiringPerHour));

        new AutoExpireConsentsActivity(repository).execute();

        final List<String> firstHourPartitionKeys = consentsExpiringFirstHour.stream()
            .map(ActiveConsentWithExpiryTime::id).toList();
        final List<String> currentHourPartitionKeys = consentsExpiringCurrentHour.stream()
            .map(ActiveConsentWithExpiryTime::id).toList();

        final int expectedNumExpiryHoursProcessed = AutoExpireConsentsActivity.NUMBER_PAST_DAYS_TO_EXPIRE_CONSENTS * 24;
        verify(repository, times(expectedNumExpiryHoursProcessed))
            .getActiveConsentsWithExpiryHour(anyString(), eq(Optional.empty()));
        verify(repository).getActiveConsentsWithExpiryHour(firstExpiryHour, Optional.of(firstHourPartitionKeys.get(2)));
        verify(repository).getActiveConsentsWithExpiryHour(firstExpiryHour, Optional.of(firstHourPartitionKeys.get(4)));
        verify(repository, never()).getActiveConsentsWithExpiryHour(currentExpiryHour, Optional.of(currentHourPartitionKeys.get(2)));

        firstHourPartitionKeys.forEach(partitionKey -> verify(repository).expireConsent(partitionKey, "2"));
        verify(repository).expireConsent(currentHourPartitionKeys.get(0), "2");
        currentHourPartitionKeys.subList(1, currentHourPartitionKeys.size()).forEach(partitionKey ->
            verify(repository, never()).expireConsent(partitionKey, "2"));
    }

    private void validateNoConsentsExpired(final ConsentRepository repository) {
        verify(repository, times(AutoExpireConsentsActivity.NUMBER_PAST_DAYS_TO_EXPIRE_CONSENTS * 24))
            .getActiveConsentsWithExpiryHour(anyString(), eq(Optional.empty()));
        verify(repository, never()).expireConsent(any(), any());
    }

    private OffsetDateTime nowPlusMinutes(final int minutes) {
        return OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(minutes);
    }
}
