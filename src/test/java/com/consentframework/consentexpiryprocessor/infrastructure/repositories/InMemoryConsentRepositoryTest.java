package com.consentframework.consentexpiryprocessor.infrastructure.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.consentframework.consentexpiryprocessor.domain.entities.ActiveConsentWithExpiryTime;
import com.consentframework.consentexpiryprocessor.testcommon.utils.ActiveConsentWithExpiryTimeGenerator;
import com.consentframework.shared.api.domain.pagination.ListPage;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class InMemoryConsentRepositoryTest {
    private static final List<ActiveConsentWithExpiryTime> TEST_CONSENTS_WITH_MIXED_EXPIRY_TIMES = List.of(
        ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), OffsetDateTime.now().minusMinutes(30)),
        ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), OffsetDateTime.now().minusMinutes(1)),
        ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), OffsetDateTime.now().plusMinutes(10)),
        ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), OffsetDateTime.now().plusMinutes(20)),
        ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), OffsetDateTime.now().plusMinutes(30))
    );

    @Test
    void getActiveConsentsWithExpiryTimesWhenEmpty() {
        final InMemoryConsentRepository repository = new InMemoryConsentRepository();

        final ListPage<ActiveConsentWithExpiryTime> pageConsents = repository.getActiveConsentsWithExpiryTimes(Optional.empty());

        assertNotNull(pageConsents);
        assertTrue(pageConsents.resultsOnPage().isEmpty());
        assertTrue(pageConsents.nextPageToken().isEmpty());
    }

    @Test
    void getActiveConsentsWithExpiryTimes_firstPage() {
        final InMemoryConsentRepository repository = new InMemoryConsentRepository(TEST_CONSENTS_WITH_MIXED_EXPIRY_TIMES);

        final ListPage<ActiveConsentWithExpiryTime> pageConsents = repository.getActiveConsentsWithExpiryTimes(Optional.empty());

        assertNotNull(pageConsents);
        assertEquals(
            TEST_CONSENTS_WITH_MIXED_EXPIRY_TIMES.subList(0, InMemoryConsentRepository.MAX_PAGE_SIZE),
            pageConsents.resultsOnPage()
        );
        assertEquals(
            TEST_CONSENTS_WITH_MIXED_EXPIRY_TIMES.get(InMemoryConsentRepository.MAX_PAGE_SIZE).id(),
            pageConsents.nextPageToken().get()
        );
    }

    @Test
    void getActiveConsentsWithExpiryTimes_withNextToken() {
        final InMemoryConsentRepository repository = new InMemoryConsentRepository(TEST_CONSENTS_WITH_MIXED_EXPIRY_TIMES);

        final int startingIndex = 2;
        final Optional<String> providedNextPageToken = Optional.of(TEST_CONSENTS_WITH_MIXED_EXPIRY_TIMES.get(startingIndex).id());
        final ListPage<ActiveConsentWithExpiryTime> pageConsents = repository.getActiveConsentsWithExpiryTimes(providedNextPageToken);

        assertNotNull(pageConsents);
        assertEquals(
            TEST_CONSENTS_WITH_MIXED_EXPIRY_TIMES.subList(startingIndex, startingIndex + InMemoryConsentRepository.MAX_PAGE_SIZE),
            pageConsents.resultsOnPage()
        );
        assertEquals(
            TEST_CONSENTS_WITH_MIXED_EXPIRY_TIMES.get(startingIndex + InMemoryConsentRepository.MAX_PAGE_SIZE).id(),
            pageConsents.nextPageToken().get()
        );
    }

    @Test
    void getActiveConsentsWithExpiryTimes_lastPage() {
        final InMemoryConsentRepository repository = new InMemoryConsentRepository(TEST_CONSENTS_WITH_MIXED_EXPIRY_TIMES);

        final int startingIndex = 4;
        final Optional<String> providedNextPageToken = Optional.of(
            TEST_CONSENTS_WITH_MIXED_EXPIRY_TIMES.get(startingIndex).id());
        final ListPage<ActiveConsentWithExpiryTime> pageConsents = repository.getActiveConsentsWithExpiryTimes(providedNextPageToken);

        assertNotNull(pageConsents);
        assertEquals(
            List.of(TEST_CONSENTS_WITH_MIXED_EXPIRY_TIMES.get(startingIndex)),
            pageConsents.resultsOnPage()
        );
        assertTrue(pageConsents.nextPageToken().isEmpty());
    }

    @Test
    void expireConsent() {
        final InMemoryConsentRepository repository = new InMemoryConsentRepository(TEST_CONSENTS_WITH_MIXED_EXPIRY_TIMES);

        repository.expireConsent(TEST_CONSENTS_WITH_MIXED_EXPIRY_TIMES.get(1).id(), "2");
        repository.expireConsent(TEST_CONSENTS_WITH_MIXED_EXPIRY_TIMES.get(2).id(), "2");
        repository.expireConsent(TEST_CONSENTS_WITH_MIXED_EXPIRY_TIMES.get(4).id(), "2");

        final List<ActiveConsentWithExpiryTime> activeConsents = repository
            .getActiveConsentsWithExpiryTimes(Optional.empty())
            .resultsOnPage();
        assertEquals(2, activeConsents.size());
        assertEquals(TEST_CONSENTS_WITH_MIXED_EXPIRY_TIMES.get(0), activeConsents.get(0));
        assertEquals(TEST_CONSENTS_WITH_MIXED_EXPIRY_TIMES.get(3), activeConsents.get(1));
    }
}
