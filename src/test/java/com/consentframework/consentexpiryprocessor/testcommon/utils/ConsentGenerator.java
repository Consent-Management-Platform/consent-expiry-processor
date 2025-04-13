package com.consentframework.consentexpiryprocessor.testcommon.utils;

import com.consentframework.consentexpiryprocessor.testcommon.constants.TestConstants;
import com.consentframework.consentmanagement.api.models.Consent;

import java.time.OffsetDateTime;

/**
 * Utility class for generating test consents.
 */
public class ConsentGenerator {
    /**
     * Generates a test consent with the given consentId and expiryTime.
     *
     * @param consentId The consentId associated with the given service-user-consent.
     * @param expiryTime The consent's expiry time.
     * @return The generated consent.
     */
    public static Consent generateConsent(final String consentId, final OffsetDateTime expiryTime) {
        return new Consent()
            .serviceId(TestConstants.TEST_SERVICE_ID)
            .userId(TestConstants.TEST_USER_ID)
            .consentId(consentId)
            .consentVersion(1)
            .status(TestConstants.TEST_CONSENT_STATUS)
            .consentType(TestConstants.TEST_CONSENT_TYPE)
            .consentData(TestConstants.TEST_CONSENT_DATA)
            .expiryTime(expiryTime);
    }
}
