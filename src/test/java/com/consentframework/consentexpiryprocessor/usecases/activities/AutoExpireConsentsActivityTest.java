package com.consentframework.consentexpiryprocessor.usecases.activities;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.consentframework.consentexpiryprocessor.domain.repositories.ConsentRepository;
import com.consentframework.consentexpiryprocessor.infrastructure.repositories.InMemoryConsentRepository;
import com.consentframework.consentexpiryprocessor.testcommon.utils.ConsentGenerator;
import com.consentframework.consentmanagement.api.models.Consent;
import com.consentframework.shared.api.domain.pagination.ListPage;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
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
        doReturn(null).when(repository).getActiveConsentsWithExpiryTimes(expectedGetActiveConsentsInput);

        new AutoExpireConsentsActivity(repository).execute();

        validateNoConsentsExpired(repository);
    }

    @Test
    void executeWhenNullResultsOnPage() {
        final InMemoryConsentRepository repository = mock(InMemoryConsentRepository.class);
        final Optional<String> expectedGetActiveConsentsInput = Optional.empty();
        final ListPage<Consent> mockPageConsents = new ListPage<>(null, Optional.empty());
        doReturn(mockPageConsents).when(repository).getActiveConsentsWithExpiryTimes(expectedGetActiveConsentsInput);

        new AutoExpireConsentsActivity(repository).execute();

        validateNoConsentsExpired(repository);
    }

    @Test
    void executeWhenMultiplePageResults() {
        final List<Consent> consents = List.of(
            ConsentGenerator.generateConsent(UUID.randomUUID().toString(), nowPlusMinutes(-60 * 24 * 7)),
            ConsentGenerator.generateConsent(UUID.randomUUID().toString(), nowPlusMinutes(-60 * 24 * 2)),
            ConsentGenerator.generateConsent(UUID.randomUUID().toString(), nowPlusMinutes(-60 * 24)),
            ConsentGenerator.generateConsent(UUID.randomUUID().toString(), nowPlusMinutes(-60 * 2)),
            ConsentGenerator.generateConsent(UUID.randomUUID().toString(), nowPlusMinutes(-1)),
            ConsentGenerator.generateConsent(UUID.randomUUID().toString(), nowPlusMinutes(10)),
            ConsentGenerator.generateConsent(UUID.randomUUID().toString(), nowPlusMinutes(240))
        );
        final InMemoryConsentRepository repository = spy(new InMemoryConsentRepository(consents));

        new AutoExpireConsentsActivity(repository).execute();

        final List<String> partitionKeys = consents.stream().map(repository::getPartitionKey).toList();

        verify(repository).getActiveConsentsWithExpiryTimes(Optional.empty());
        verify(repository).getActiveConsentsWithExpiryTimes(Optional.of(partitionKeys.get(2)));
        verify(repository).getActiveConsentsWithExpiryTimes(Optional.of(partitionKeys.get(4)));
        verify(repository, never()).getActiveConsentsWithExpiryTimes(Optional.of(partitionKeys.get(6)));

        partitionKeys.subList(0, 5).forEach(partitionKey -> verify(repository).expireConsent(partitionKey, "2"));
        partitionKeys.subList(5, consents.size()).forEach(partitionKey -> verify(repository, never()).expireConsent(partitionKey, "2"));
    }

    private void validateNoConsentsExpired(final ConsentRepository repository) {
        verify(repository).getActiveConsentsWithExpiryTimes(Optional.empty());
        verify(repository, never()).expireConsent(any(), any());
    }

    private OffsetDateTime nowPlusMinutes(final int minutes) {
        return OffsetDateTime.now().withOffsetSameLocal(ZoneOffset.UTC).plusMinutes(minutes);
    }
}
