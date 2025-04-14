package com.consentframework.consentexpiryprocessor.infrastructure.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.consentframework.consentexpiryprocessor.domain.repositories.ConsentRepository;
import com.consentframework.consentexpiryprocessor.testcommon.constants.TestConstants;
import org.junit.jupiter.api.Test;

import java.util.Optional;

class DynamoDbConsentRepositoryTest {
    // TODO: Update test once implement getActiveConsentsWithExpiryTimes.
    @Test
    void getActiveConsentsWithExpiryTimes() {
        final ConsentRepository repository = new DynamoDbConsentRepository();
        assertThrows(UnsupportedOperationException.class, () ->
            repository.getActiveConsentsWithExpiryTimes(Optional.empty()));
    }

    // TODO: Update test once implement expireConsent.
    @Test
    void expireConsent() {
        final ConsentRepository repository = new DynamoDbConsentRepository();
        assertThrows(UnsupportedOperationException.class, () ->
            repository.expireConsent(TestConstants.TEST_PARTITION_KEY, "2"));
    }

    @Test
    void getPartitionKey() {
        final ConsentRepository repository = new DynamoDbConsentRepository();
        final String partitionKey = repository.getPartitionKey(TestConstants.TEST_CONSENT);
        assertEquals(TestConstants.TEST_PARTITION_KEY, partitionKey);
    }
}
