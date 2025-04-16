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
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
        final List<ActiveConsentWithExpiryTime> consentsExpiringThisHour = List.of(
            ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), nowPlusMinutes(-50)),
            ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), nowPlusMinutes(-40)),
            ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), nowPlusMinutes(-30)),
            ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), nowPlusMinutes(-20)),
            ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), nowPlusMinutes(-10)),
            ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), nowPlusMinutes(10)),
            ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), nowPlusMinutes(240))
        );
        final String expiryHour = consentsExpiringThisHour.get(0).expiryHour();
        final Map<String, List<ActiveConsentWithExpiryTime>> consentsExpiringPerHour = Map.of(
            expiryHour, consentsExpiringThisHour);
        final InMemoryConsentRepository repository = spy(new InMemoryConsentRepository(consentsExpiringPerHour));

        new AutoExpireConsentsActivity(repository).execute();

        final List<String> partitionKeys = consentsExpiringThisHour.stream().map(ActiveConsentWithExpiryTime::id).toList();

        final int expectedNumExpiryHoursProcessed = AutoExpireConsentsActivity.NUMBER_PAST_DAYS_TO_EXPIRE_CONSENTS * 24;
        verify(repository, times(expectedNumExpiryHoursProcessed))
            .getActiveConsentsWithExpiryHour(anyString(), eq(Optional.empty()));
        verify(repository).getActiveConsentsWithExpiryHour(expiryHour, Optional.of(partitionKeys.get(2)));
        verify(repository).getActiveConsentsWithExpiryHour(expiryHour, Optional.of(partitionKeys.get(4)));
        verify(repository, never()).getActiveConsentsWithExpiryHour(expiryHour, Optional.of(partitionKeys.get(6)));

        partitionKeys.subList(0, 5).forEach(partitionKey -> verify(repository).expireConsent(partitionKey, "2"));
        partitionKeys.subList(5, consentsExpiringThisHour.size()).forEach(partitionKey ->
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
