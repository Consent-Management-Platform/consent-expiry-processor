package com.consentframework.consentexpiryprocessor.domain.constants;

/**
 * ActiveConsentWithExpiryTime attribute names.
 */
public enum ActiveConsentWithExpiryTimeAttributeName {
    ID("id"),
    CONSENT_VERSION("consentVersion"),
    EXPIRY_HOUR("expiryHour"),
    EXPIRY_TIME_ID("expiryTimeId");

    private final String value;

    private ActiveConsentWithExpiryTimeAttributeName(final String value) {
        this.value = value;
    }

    /**
     * Return attribute name.
     *
     * @return attribute name
     */
    public String getValue() {
        return value;
    }
}
