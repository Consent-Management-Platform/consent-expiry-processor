package com.consentframework.consentexpiryprocessor.infrastructure.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.consentframework.consentexpiryprocessor.domain.entities.ActiveConsentWithExpiryTime;
import com.consentframework.consentexpiryprocessor.testcommon.constants.TestConstants;
import com.consentframework.consentexpiryprocessor.testcommon.utils.ActiveConsentWithExpiryTimeGenerator;
import com.consentframework.shared.api.domain.pagination.ListPage;
import com.consentframework.shared.api.infrastructure.mappers.DynamoDbConsentExpiryTimeConverter;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class InMemoryConsentRepositoryTest {
    private static final OffsetDateTime FIRST_HOUR_DATETIME = OffsetDateTime.now().minusHours(2).truncatedTo(ChronoUnit.HOURS);
    private static final String FIRST_EXPIRY_HOUR = DynamoDbConsentExpiryTimeConverter.toExpiryHour(FIRST_HOUR_DATETIME);
    private static final String SECOND_EXPIRY_HOUR = DynamoDbConsentExpiryTimeConverter.toExpiryHour(FIRST_HOUR_DATETIME.plusHours(1));

    private static final List<ActiveConsentWithExpiryTime> CONSENTS_EXPIRING_FIRST_HOUR = List.of(
        ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), FIRST_HOUR_DATETIME.plusMinutes(1)),
        ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), FIRST_HOUR_DATETIME.plusMinutes(10)),
        ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), FIRST_HOUR_DATETIME.plusMinutes(20)),
        ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), FIRST_HOUR_DATETIME.plusMinutes(30)),
        ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), FIRST_HOUR_DATETIME.plusMinutes(40))
    );
    private static final List<ActiveConsentWithExpiryTime> CONSENTS_EXPIRING_SECOND_HOUR = List.of(
        ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), FIRST_HOUR_DATETIME.plusMinutes(70)),
        ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), FIRST_HOUR_DATETIME.plusMinutes(80)),
        ActiveConsentWithExpiryTimeGenerator.generate(UUID.randomUUID().toString(), FIRST_HOUR_DATETIME.plusMinutes(90))
    );
    private static final Map<String, List<ActiveConsentWithExpiryTime>> TEST_CONSENTS_BY_EXPIRY_HOUR = Map.of(
        FIRST_EXPIRY_HOUR, CONSENTS_EXPIRING_FIRST_HOUR,
        SECOND_EXPIRY_HOUR, CONSENTS_EXPIRING_SECOND_HOUR
    );

    @Test
    void getActiveConsentsWithExpiryTimesWhenEmpty() {
        final InMemoryConsentRepository repository = new InMemoryConsentRepository();

        final ListPage<ActiveConsentWithExpiryTime> pageConsents = repository.getActiveConsentsWithExpiryHour(
            TestConstants.TEST_EXPIRY_HOUR, Optional.empty());

        assertNotNull(pageConsents);
        assertTrue(pageConsents.resultsOnPage().isEmpty());
        assertTrue(pageConsents.nextPageToken().isEmpty());
    }

    @Test
    void getActiveConsentsWithExpiryTimes_firstPage() {
        final InMemoryConsentRepository repository = new InMemoryConsentRepository(TEST_CONSENTS_BY_EXPIRY_HOUR);

        final ListPage<ActiveConsentWithExpiryTime> pageConsents = repository.getActiveConsentsWithExpiryHour(
            FIRST_EXPIRY_HOUR, Optional.empty());

        assertNotNull(pageConsents);
        assertEquals(
            CONSENTS_EXPIRING_FIRST_HOUR.subList(0, InMemoryConsentRepository.MAX_PAGE_SIZE),
            pageConsents.resultsOnPage()
        );
        assertEquals(
            CONSENTS_EXPIRING_FIRST_HOUR.get(InMemoryConsentRepository.MAX_PAGE_SIZE).id(),
            pageConsents.nextPageToken().get()
        );
    }

    @Test
    void getActiveConsentsWithExpiryTimes_withNextToken() {
        final InMemoryConsentRepository repository = new InMemoryConsentRepository(TEST_CONSENTS_BY_EXPIRY_HOUR);

        final int startingIndex = 2;
        final Optional<String> providedNextPageToken = Optional.of(CONSENTS_EXPIRING_FIRST_HOUR.get(startingIndex).id());
        final ListPage<ActiveConsentWithExpiryTime> pageConsents = repository.getActiveConsentsWithExpiryHour(
            FIRST_EXPIRY_HOUR, providedNextPageToken);

        assertNotNull(pageConsents);
        assertEquals(
            CONSENTS_EXPIRING_FIRST_HOUR.subList(startingIndex, startingIndex + InMemoryConsentRepository.MAX_PAGE_SIZE),
            pageConsents.resultsOnPage()
        );
        assertEquals(
            CONSENTS_EXPIRING_FIRST_HOUR.get(startingIndex + InMemoryConsentRepository.MAX_PAGE_SIZE).id(),
            pageConsents.nextPageToken().get()
        );
    }

    @Test
    void getActiveConsentsWithExpiryTimes_lastPage() {
        final InMemoryConsentRepository repository = new InMemoryConsentRepository(TEST_CONSENTS_BY_EXPIRY_HOUR);

        final int startingIndex = 4;
        final Optional<String> providedNextPageToken = Optional.of(
            CONSENTS_EXPIRING_FIRST_HOUR.get(startingIndex).id());
        final ListPage<ActiveConsentWithExpiryTime> pageConsents = repository.getActiveConsentsWithExpiryHour(
            FIRST_EXPIRY_HOUR, providedNextPageToken);

        assertNotNull(pageConsents);
        assertEquals(
            List.of(CONSENTS_EXPIRING_FIRST_HOUR.get(startingIndex)),
            pageConsents.resultsOnPage()
        );
        assertTrue(pageConsents.nextPageToken().isEmpty());
    }

    @Test
    void expireConsent() {
        final InMemoryConsentRepository repository = new InMemoryConsentRepository(TEST_CONSENTS_BY_EXPIRY_HOUR);

        repository.expireConsent(CONSENTS_EXPIRING_FIRST_HOUR.get(1).id(), "2");
        repository.expireConsent(CONSENTS_EXPIRING_FIRST_HOUR.get(2).id(), "2");
        repository.expireConsent(CONSENTS_EXPIRING_FIRST_HOUR.get(4).id(), "2");

        final List<ActiveConsentWithExpiryTime> activeConsents = repository
            .getActiveConsentsWithExpiryHour(FIRST_EXPIRY_HOUR, Optional.empty())
            .resultsOnPage();
        assertEquals(2, activeConsents.size());
        assertEquals(CONSENTS_EXPIRING_FIRST_HOUR.get(0), activeConsents.get(0));
        assertEquals(CONSENTS_EXPIRING_FIRST_HOUR.get(3), activeConsents.get(1));
    }
}
