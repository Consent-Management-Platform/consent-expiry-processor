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
        final int firstIndex = pageToken.map(Integer::parseInt).orElse(0);
        final int nextIndex = Math.min(firstIndex + MAX_PAGE_SIZE, consents.size());

        final List<Consent> consentsOnPage = consents.subList(firstIndex, nextIndex);
        final Optional<String> nextPageToken = getNextPageToken(nextIndex);

        return new ListPage<>(consentsOnPage, nextPageToken);
    }

    private Optional<String> getNextPageToken(final int nextIndex) {
        if (consents.size() > nextIndex) {
            return Optional.of(String.valueOf(nextIndex));
        }
        return Optional.empty();
    }

    /**
     * Updates the status of a consent to expired.
     */
    @Override
    public void expireConsent(final String id, final String updatedVersion) {
        final Consent consent = consentsByPartitionKey.get(id);
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
