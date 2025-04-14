package com.consentframework.consentexpiryprocessor.domain.repositories;

import com.consentframework.consentmanagement.api.models.Consent;
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

    // In-memory list of consents
    final List<Consent> consents = new ArrayList<>();

    // Map from consent partition key to Consent object
    final Map<String, Consent> consentsByPartitionKey = new HashMap<>();

    /**
     * Constructs an in-memory consent repository.
     */
    public InMemoryConsentRepository() {}

    /**
     * Constructs an in-memory consent repository pre-populated with a list of consents.
     *
     * @param consents The list of consents to add to the repository.
     */
    public InMemoryConsentRepository(final List<Consent> consents) {
        consents.forEach(consent -> {
            this.consents.add(consent);

            final String partitionKey = getPartitionKey(consent);
            consentsByPartitionKey.put(partitionKey, consent);
        });
    }

    /**
     * Retrieves a paginated list of active consents with non-null expiry times.
     */
    @Override
    public ListPage<Consent> getActiveConsentsWithExpiryTimes(final Optional<String> pageToken) {
        final int firstIndex = pageToken.map(partitionKey -> consents.indexOf(consentsByPartitionKey.get(partitionKey)))
            .orElse(0);
        final int nextIndex = Math.min(firstIndex + MAX_PAGE_SIZE, consents.size());

        // Copy to a new list to avoid callers indirectly modifying the repository's consent list.
        final List<Consent> consentsOnPage = List.copyOf(consents.subList(firstIndex, nextIndex));
        final Optional<String> nextPageToken = getNextPageToken(nextIndex);

        return new ListPage<>(consentsOnPage, nextPageToken);
    }

    private Optional<String> getNextPageToken(final int nextIndex) {
        if (consents.size() > nextIndex) {
            final String nextConsentPartitionKey = getPartitionKey(consents.get(nextIndex));
            return Optional.of(nextConsentPartitionKey);
        }
        return Optional.empty();
    }

    /**
     * Updates the status of a consent to expired.
     */
    @Override
    public void expireConsent(final String id, final String updatedVersion) {
        final Consent consent = consentsByPartitionKey.get(id);
        consentsByPartitionKey.remove(id);
        consents.remove(consent);
    }

    /**
     * Return data store partition key for a consent.
     */
    @Override
    public String getPartitionKey(final Consent consent) {
        return String.format("%s|%s|%s", consent.getServiceId(), consent.getUserId(), consent.getConsentId());
    }
}
