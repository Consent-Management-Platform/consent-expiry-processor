package com.consentframework.consentexpiryprocessor.testcommon.constants;

import com.consentframework.consentmanagement.api.models.Consent;
import com.consentframework.consentmanagement.api.models.ConsentStatus;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * Test constants.
 */
public class TestConstants {
    private TestConstants() {}

    public static final String TEST_SERVICE_ID = "TestServiceId";
    public static final String TEST_USER_ID = "TestUserId";
    public static final String TEST_CONSENT_ID = "TestConsentId";
    public static final String TEST_PARTITION_KEY = String.format("%s|%s|%s", TEST_SERVICE_ID, TEST_USER_ID, TEST_CONSENT_ID);

    public static final ConsentStatus TEST_CONSENT_STATUS = ConsentStatus.ACTIVE;
    public static final String TEST_CONSENT_TYPE = "TestConsentType";
    public static final Map<String, String> TEST_CONSENT_DATA = Map.of(
        "testKey1", "testValue1",
        "testKey2", "testValue2"
    );
    public static OffsetDateTime TEST_EXPIRY_TIME = OffsetDateTime.now().withOffsetSameLocal(ZoneOffset.UTC);

    public static final Consent TEST_CONSENT = new Consent()
        .serviceId(TEST_SERVICE_ID)
        .userId(TEST_USER_ID)
        .consentId(TEST_CONSENT_ID)
        .consentVersion(1)
        .status(TEST_CONSENT_STATUS)
        .consentType(TEST_CONSENT_TYPE)
        .consentData(TEST_CONSENT_DATA)
        .expiryTime(TEST_EXPIRY_TIME);
}
