package com.consentframework.consentexpiryprocessor.testcommon.utils;

import com.consentframework.consentexpiryprocessor.domain.entities.ActiveConsentWithExpiryTime;
import com.consentframework.shared.api.infrastructure.mappers.DynamoDbConsentExpiryTimeConverter;

import java.time.OffsetDateTime;

/**
 * Utility class for generating test ActiveConsentWithExpiryTime objects.
 */
public final class ActiveConsentWithExpiryTimeGenerator {
    /**
     * Generates an ActiveConsentWithExpiryTime object with the given id and expiryTime.
     *
     * @param id The consent partition key
     * @param expiryTime The consent expiry time
     * @return The generated ActiveConsentWithExpiryTime
     */
    public static ActiveConsentWithExpiryTime generate(final String id, final OffsetDateTime expiryTime) {
        return ActiveConsentWithExpiryTime.builder()
            .id(id)
            .consentVersion(1)
            .expiryHour(DynamoDbConsentExpiryTimeConverter.toExpiryHour(expiryTime))
            .expiryTimeId(DynamoDbConsentExpiryTimeConverter.toExpiryTimeId(expiryTime, id))
            .build();
    }
}
