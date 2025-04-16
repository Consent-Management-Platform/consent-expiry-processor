package com.consentframework.consentexpiryprocessor.testcommon.constants;

/**
 * Test constants.
 */
public class TestConstants {
    private TestConstants() {}

    public static final String TEST_SERVICE_ID = "TestServiceId";
    public static final String TEST_USER_ID = "TestUserId";
    public static final String TEST_CONSENT_ID = "TestConsentId";
    public static final String TEST_CONSENT_ID_2 = "TestConsentId2";
    public static final String TEST_CONSENT_ID_3 = "TestConsentId3";
    public static final String TEST_PARTITION_KEY = String.format("%s|%s|%s", TEST_SERVICE_ID, TEST_USER_ID, TEST_CONSENT_ID);
    public static final String TEST_PARTITION_KEY_2 = String.format("%s|%s|%s", TEST_SERVICE_ID, TEST_USER_ID, TEST_CONSENT_ID_2);
    public static final String TEST_PARTITION_KEY_3 = String.format("%s|%s|%s", TEST_SERVICE_ID, TEST_USER_ID, TEST_CONSENT_ID_3);

    public static final String TEST_EXPIRY_HOUR = "2011-12-03T10:00Z";
    public static final String TEST_EXPIRY_TIME_ID = "2011-12-03T10:15:12Z|" + TEST_PARTITION_KEY;
}
