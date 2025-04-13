package com.consentframework.consentexpiryprocessor.domain.repositories;

import com.consentframework.consentmanagement.api.models.Consent;
import com.consentframework.shared.api.domain.pagination.ListPage;

import java.util.Optional;

/**
 * Interface for the consent repository, defining methods for querying and updating consents.
 */
public interface ConsentRepository {
    /**
     * Retrieves a paginated list of active consents with non-null expiry times.
     *
     * @param pageToken The page token for pagination, null if retrieving the first page.
     * @return paginated list of consents, or null if no results found.
     */
    ListPage<Consent> getActiveConsentsWithExpiryTimes(final Optional<String> pageToken);

    /**
     * Updates the status of a consent to expired.
     *
     * @param id The partition key of the consent record.
     * @param updatedVersion The updated version of the consent record.
     */
    void expireConsent(final String id, final String updatedVersion);

    /**
     * Generates the partition key for a consent record.
     *
     * @param consent The consent record.
     * @return The partition key for the consent record.
     */
    String getPartitionKey(final Consent consent);
}
