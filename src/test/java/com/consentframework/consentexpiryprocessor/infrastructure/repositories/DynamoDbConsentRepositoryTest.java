package com.consentframework.consentexpiryprocessor.infrastructure.repositories;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.consentframework.consentexpiryprocessor.domain.repositories.ConsentRepository;
import com.consentframework.consentexpiryprocessor.testcommon.constants.TestConstants;
import com.consentframework.shared.api.infrastructure.entities.DynamoDbServiceUserConsent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.util.Optional;

class DynamoDbConsentRepositoryTest {
    private DynamoDbTable<DynamoDbServiceUserConsent> consentTable;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setup() {
        consentTable = mock(DynamoDbTable.class);
    }

    // TODO: Update test once implement getActiveConsentsWithExpiryTimes.
    @Test
    void getActiveConsentsWithExpiryTimes() {
        final ConsentRepository repository = new DynamoDbConsentRepository(consentTable);
        assertThrows(UnsupportedOperationException.class, () ->
            repository.getActiveConsentsWithExpiryTimes(Optional.empty()));
    }

    // TODO: Update test once implement expireConsent.
    @Test
    void expireConsent() {
        final ConsentRepository repository = new DynamoDbConsentRepository(consentTable);
        assertThrows(UnsupportedOperationException.class, () ->
            repository.expireConsent(TestConstants.TEST_PARTITION_KEY, "2"));
    }
}
