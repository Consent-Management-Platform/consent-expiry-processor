package com.consentframework.consentexpiryprocessor.domain.repositories;

import com.consentframework.consentmanagement.api.models.Consent;
import com.consentframework.shared.api.domain.pagination.ListPage;

/**
 * Interface for the consent repository, defining methods for querying and updating consents.
 */
public interface ConsentRepository {
    /**
     * Retrieves a paginated list of active consents with non-null expiry times.
     *
     * @return paginated list of consents.
     */
    ListPage<Consent> getActiveConsentsWithExpiryTimes();

    /**
     * Updates the status of a consent to expired.
     *
     * @param id The partition key of the consent record.
     * @param updatedVersion The updated version of the consent record.
     */
    void expireConsent(final String id, final String updatedVersion);
}
