package com.consentframework.consentexpiryprocessor.testcommon.constants;

/**
 * Test constants.
 */
public class TestConstants {
    private TestConstants() {}

    public static final String TEST_SERVICE_ID = "TestServiceId";
    public static final String TEST_USER_ID = "TestUserId";
    public static final String TEST_CONSENT_ID = "TestConsentId";
    public static final String TEST_PARTITION_KEY = String.format("%s|%s|%s", TEST_SERVICE_ID, TEST_USER_ID, TEST_CONSENT_ID);
}
