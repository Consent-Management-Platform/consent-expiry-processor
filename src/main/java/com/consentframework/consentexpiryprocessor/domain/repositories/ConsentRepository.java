package com.consentframework.consentexpiryprocessor.domain.repositories;

import com.consentframework.consentexpiryprocessor.domain.entities.ActiveConsentWithExpiryTime;
import com.consentframework.shared.api.domain.pagination.ListPage;

import java.util.Optional;

/**
 * Interface for the consent repository, defining methods for querying and updating consents.
 */
public interface ConsentRepository {
    /**
     * Retrieves a paginated list of active consents with non-null expiry times.
     *
     * @param expiryHour The expiry hour to query for, with format "2021-12-31T23:00Z".
     * @param pageToken The page token for pagination, null if retrieving the first page.
     * @return paginated list of consents, or null if no results found.
     */
    ListPage<ActiveConsentWithExpiryTime> getActiveConsentsWithExpiryHour(final String expiryHour, final Optional<String> pageToken);

    /**
     * Updates the status of a consent to expired.
     *
     * @param id The partition key of the consent record.
     * @param updatedVersion The updated version of the consent record.
     */
    void expireConsent(final String id, final String updatedVersion);
}
