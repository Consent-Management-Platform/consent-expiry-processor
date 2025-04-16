package com.consentframework.consentexpiryprocessor.infrastructure.repositories;

import com.consentframework.consentexpiryprocessor.domain.entities.ActiveConsentWithExpiryTime;
import com.consentframework.consentexpiryprocessor.domain.repositories.ConsentRepository;
import com.consentframework.shared.api.domain.pagination.ListPage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory implementation of the consent repository, used for testing.
 *
 * Once have implemented the DynamoDB consent repository, this class will be moved to the test directory.
 */
public class InMemoryConsentRepository implements ConsentRepository {
    public static final Integer MAX_PAGE_SIZE = 2;

    // In-memory map from expiry hour to active consents with expiry times during that hour
    final Map<String, List<ActiveConsentWithExpiryTime>> activeConsentsPerExpiryHour = new HashMap<>();

    // Map from consent partition key to consent object, enables expiring consents by id
    final Map<String, ActiveConsentWithExpiryTime> consentsByPartitionKey = new HashMap<>();

    /**
     * Constructs an in-memory consent repository.
     */
    public InMemoryConsentRepository() {}

    /**
     * Constructs an in-memory consent repository pre-populated with consents.
     */
    public InMemoryConsentRepository(final Map<String, List<ActiveConsentWithExpiryTime>> consentsByExpiryHour) {
        consentsByExpiryHour.forEach((expiryHour, consents) -> {
            // Make a mutable copy of the input list to avoid modifying the caller's list,
            // and to allow adding/removing consents from the in-memory repository.
            final List<ActiveConsentWithExpiryTime> consentsExpiringInHour = new ArrayList<>();
            consentsExpiringInHour.addAll(consents);

            activeConsentsPerExpiryHour.put(expiryHour, consentsExpiringInHour);

            consentsExpiringInHour.forEach(consent -> consentsByPartitionKey.put(consent.id(), consent));
        });
    }

    /**
     * Retrieves a paginated list of active consents with non-null expiry times.
     */
    @Override
    public ListPage<ActiveConsentWithExpiryTime> getActiveConsentsWithExpiryHour(final String expiryHour,
            final Optional<String> pageToken) {
        final List<ActiveConsentWithExpiryTime> consents = activeConsentsPerExpiryHour.get(expiryHour);
        if (consents == null) {
            return new ListPage<>(List.of(), Optional.empty());
        }

        final int firstIndex = pageToken.map(partitionKey -> consents.indexOf(consentsByPartitionKey.get(partitionKey)))
            .orElse(0);
        final int nextIndex = Math.min(firstIndex + MAX_PAGE_SIZE, consents.size());

        // Copy to a new list to avoid callers indirectly modifying the repository's consent list.
        final List<ActiveConsentWithExpiryTime> consentsOnPage = List.copyOf(consents.subList(firstIndex, nextIndex));
        final Optional<String> nextPageToken = getNextPageToken(expiryHour, nextIndex);

        return new ListPage<>(consentsOnPage, nextPageToken);
    }

    private Optional<String> getNextPageToken(final String expiryHour, final int nextIndex) {
        final List<ActiveConsentWithExpiryTime> consents = activeConsentsPerExpiryHour.get(expiryHour);
        if (consents == null || consents.size() <= nextIndex) {
            return Optional.empty();
        }

        final String nextConsentPartitionKey = consents.get(nextIndex).id();
        return Optional.of(nextConsentPartitionKey);
    }

    /**
     * Updates the status of a consent to expired.
     */
    @Override
    public void expireConsent(final String id, final String updatedVersion) {
        final ActiveConsentWithExpiryTime consent = consentsByPartitionKey.get(id);
        consentsByPartitionKey.remove(id);

        final String expiryHour = consent.expiryHour();
        final List<ActiveConsentWithExpiryTime> consentsExpiringThisHour = activeConsentsPerExpiryHour.get(expiryHour);
        if (consentsExpiringThisHour != null) {
            consentsExpiringThisHour.remove(consent);
        }
    }
}
