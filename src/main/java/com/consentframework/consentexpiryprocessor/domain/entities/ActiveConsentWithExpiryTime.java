package com.consentframework.consentexpiryprocessor.domain.entities;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

/**
 * Represents an ActiveConsentsByExpiryHour index item, which has a subset of Consent attributes.
 */
@Immutable
@Style(
    // Omit add(*) methods so they aren't interpreted as table attributes
    builtinContainerAttributes = false,
    // Omit copy(*) methods so they aren't interpreted as table attributes
    defaults = @Immutable(copy = false),
    // Omit from(*) methods so they aren't interpreted as table attributes
    from = "",
    // Enable strict builder mode to prevent initialization errors
    strictBuilder = true,
    // Have Builder return original class instead of implementation
    overshadowImplementation = true
)
public interface ActiveConsentWithExpiryTime {
    static Builder builder() {
        return new Builder();
    }

    /**
     * ActiveConsentWithExpiryTime Builder class, intentionally empty.
     */
    class Builder extends ImmutableActiveConsentWithExpiryTime.Builder {}

    String id();

    Integer consentVersion();

    String expiryHour();

    String expiryTimeId();
}
