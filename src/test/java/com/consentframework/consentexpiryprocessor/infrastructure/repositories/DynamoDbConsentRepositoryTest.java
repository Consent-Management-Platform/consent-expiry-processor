package com.consentframework.consentexpiryprocessor.infrastructure.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.consentframework.consentexpiryprocessor.domain.constants.ActiveConsentWithExpiryTimeAttributeName;
import com.consentframework.consentexpiryprocessor.domain.entities.ActiveConsentWithExpiryTime;
import com.consentframework.consentexpiryprocessor.domain.repositories.ConsentRepository;
import com.consentframework.consentexpiryprocessor.testcommon.constants.TestConstants;
import com.consentframework.shared.api.domain.pagination.ListPage;
import com.consentframework.shared.api.infrastructure.entities.DynamoDbActiveConsentWithExpiryTime;
import com.consentframework.shared.api.infrastructure.mappers.DynamoDbConsentExpiryTimeConverter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

class DynamoDbConsentRepositoryTest {
    private static final OffsetDateTime FUTURE_EXPIRY_TIME = OffsetDateTime.now().plusMinutes(10);
    private static final List<DynamoDbActiveConsentWithExpiryTime> ACTIVE_CONSENTS_WITH_EXPIRY = List.of(
        DynamoDbActiveConsentWithExpiryTime.builder()
            .id(TestConstants.TEST_PARTITION_KEY)
            .consentVersion(1)
            .expiryHour(TestConstants.TEST_EXPIRY_HOUR)
            .expiryTimeId(TestConstants.TEST_EXPIRY_TIME_ID)
            .build(),
        DynamoDbActiveConsentWithExpiryTime.builder()
            .id(TestConstants.TEST_PARTITION_KEY_2)
            .consentVersion(3)
            .expiryHour(TestConstants.TEST_EXPIRY_HOUR)
            .expiryTimeId(TestConstants.TEST_EXPIRY_TIME_ID)
            .build(),
        DynamoDbActiveConsentWithExpiryTime.builder()
            .id(TestConstants.TEST_PARTITION_KEY_3)
            .consentVersion(3)
            .expiryHour(DynamoDbConsentExpiryTimeConverter.toExpiryHour(FUTURE_EXPIRY_TIME))
            .expiryTimeId(DynamoDbConsentExpiryTimeConverter.toExpiryTimeId(FUTURE_EXPIRY_TIME, TestConstants.TEST_PARTITION_KEY_3))
            .build()
    );

    private static final String NEXT_TOKEN_ID = ACTIVE_CONSENTS_WITH_EXPIRY.get(2).id();
    private static final Integer NEXT_TOKEN_CONSENT_VERSION = ACTIVE_CONSENTS_WITH_EXPIRY.get(2).consentVersion();
    private static final String NEXT_TOKEN_EXPIRY_HOUR = ACTIVE_CONSENTS_WITH_EXPIRY.get(2).expiryHour();
    private static final String NEXT_TOKEN_EXPIRY_TIME_ID = ACTIVE_CONSENTS_WITH_EXPIRY.get(2).expiryTimeId();

    private static final String NEXT_TOKEN_JSON_STRING = String.format(
        "{\"%s\":\"%s\",\"%s\":%d,\"%s\":\"%s\",\"%s\":\"%s\"}",
        ActiveConsentWithExpiryTimeAttributeName.ID.getValue(),
        NEXT_TOKEN_ID,
        ActiveConsentWithExpiryTimeAttributeName.CONSENT_VERSION.getValue(),
        NEXT_TOKEN_CONSENT_VERSION,
        ActiveConsentWithExpiryTimeAttributeName.EXPIRY_HOUR.getValue(),
        NEXT_TOKEN_EXPIRY_HOUR,
        ActiveConsentWithExpiryTimeAttributeName.EXPIRY_TIME_ID.getValue(),
        NEXT_TOKEN_EXPIRY_TIME_ID
    );

    private static final Map<String, AttributeValue> NEXT_TOKEN_ATTRIBUTE_VALUE_MAP = Map.of(
        ActiveConsentWithExpiryTimeAttributeName.ID.getValue(),
        AttributeValue.builder().s(NEXT_TOKEN_ID).build(),
        ActiveConsentWithExpiryTimeAttributeName.CONSENT_VERSION.getValue(),
        AttributeValue.builder().n(NEXT_TOKEN_CONSENT_VERSION.toString()).build(),
        ActiveConsentWithExpiryTimeAttributeName.EXPIRY_HOUR.getValue(),
        AttributeValue.builder().s(NEXT_TOKEN_EXPIRY_HOUR).build(),
        ActiveConsentWithExpiryTimeAttributeName.EXPIRY_TIME_ID.getValue(),
        AttributeValue.builder().s(NEXT_TOKEN_EXPIRY_TIME_ID).build()
    );

    @Mock
    private DynamoDbTable<DynamoDbActiveConsentWithExpiryTime> consentTable;

    @Mock
    private SdkIterable<Page<DynamoDbActiveConsentWithExpiryTime>> queryResults;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setup() {
        consentTable = (DynamoDbTable<DynamoDbActiveConsentWithExpiryTime>) mock(DynamoDbTable.class);
        queryResults = (SdkIterable<Page<DynamoDbActiveConsentWithExpiryTime>>) mock(PageIterable.class);
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getActiveConsentsWithExpiryTimes_whenNullQueryResults() {
        mockIndexQueryResults(null);

        final ConsentRepository repository = new DynamoDbConsentRepository(consentTable);
        assertNull(repository.getActiveConsentsWithExpiryHour(TestConstants.TEST_EXPIRY_HOUR, Optional.of(NEXT_TOKEN_JSON_STRING)));
    }

    @Test
    void getActiveConsentsWithExpiryTimes_whenSinglePageQueryResults() {
        final Page<DynamoDbActiveConsentWithExpiryTime> pageResults = Page.builder(DynamoDbActiveConsentWithExpiryTime.class)
            .items(ACTIVE_CONSENTS_WITH_EXPIRY)
            .build();
        when(queryResults.stream()).thenReturn(Stream.of(pageResults));
        mockIndexQueryResults(queryResults);

        final ConsentRepository repository = new DynamoDbConsentRepository(consentTable);
        final ListPage<ActiveConsentWithExpiryTime> matchingConsentsPage = repository.getActiveConsentsWithExpiryHour(
            TestConstants.TEST_EXPIRY_HOUR, Optional.empty());
        assertNotNull(matchingConsentsPage);

        final List<ActiveConsentWithExpiryTime> consents = matchingConsentsPage.resultsOnPage();
        assertNotNull(consents);
        validateFieldsEqual(ACTIVE_CONSENTS_WITH_EXPIRY, consents);

        assertTrue(matchingConsentsPage.nextPageToken().isEmpty());
    }

    @Test
    void getActiveConsentsWithExpiryTimes_whenMultiplePageQueryResults() throws Exception {
        final Page<DynamoDbActiveConsentWithExpiryTime> page1 = Page.builder(DynamoDbActiveConsentWithExpiryTime.class)
            .items(ACTIVE_CONSENTS_WITH_EXPIRY)
            .lastEvaluatedKey(NEXT_TOKEN_ATTRIBUTE_VALUE_MAP)
            .build();
        final Page<DynamoDbActiveConsentWithExpiryTime> page2 = Page.builder(DynamoDbActiveConsentWithExpiryTime.class)
            .items(List.of(ACTIVE_CONSENTS_WITH_EXPIRY.get(0)))
            .build();
        when(queryResults.stream()).thenReturn(Stream.of(page1, page2));
        mockIndexQueryResults(queryResults);

        final ConsentRepository repository = new DynamoDbConsentRepository(consentTable);
        final ListPage<ActiveConsentWithExpiryTime> matchingConsentsPage = repository.getActiveConsentsWithExpiryHour(
            TestConstants.TEST_EXPIRY_HOUR, Optional.empty());
        assertNotNull(matchingConsentsPage);

        final List<ActiveConsentWithExpiryTime> consents = matchingConsentsPage.resultsOnPage();
        assertNotNull(consents);
        validateFieldsEqual(ACTIVE_CONSENTS_WITH_EXPIRY, consents);

        assertTrue(matchingConsentsPage.nextPageToken().isPresent());
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode expectedJson = objectMapper.readTree(NEXT_TOKEN_JSON_STRING);
        final JsonNode parsedJson = objectMapper.readTree(matchingConsentsPage.nextPageToken().get());
        assertEquals(expectedJson, parsedJson);
    }

    // TODO: Update test once implement expireConsent.
    @Test
    void expireConsent() {
        final ConsentRepository repository = new DynamoDbConsentRepository(consentTable);
        assertThrows(UnsupportedOperationException.class, () ->
            repository.expireConsent(TestConstants.TEST_PARTITION_KEY, "2"));
    }

    private void validateFieldsEqual(final List<DynamoDbActiveConsentWithExpiryTime> originalDbItems,
            final List<ActiveConsentWithExpiryTime> parsedConsents) {
        assertEquals(originalDbItems.size(), parsedConsents.size());
        for (int i = 0; i < originalDbItems.size(); i++) {
            validateFieldsEqual(originalDbItems.get(i), parsedConsents.get(i));
        }
    }

    private void validateFieldsEqual(final DynamoDbActiveConsentWithExpiryTime originalDbItem,
            final ActiveConsentWithExpiryTime parsedConsent) {
        assertEquals(originalDbItem.id(), parsedConsent.id());
        assertEquals(originalDbItem.consentVersion(), parsedConsent.consentVersion());
        assertEquals(originalDbItem.expiryHour(), parsedConsent.expiryHour());
        assertEquals(originalDbItem.expiryTimeId(), parsedConsent.expiryTimeId());
    }

    private void mockIndexQueryResults(final SdkIterable<Page<DynamoDbActiveConsentWithExpiryTime>> queryResults) {
        @SuppressWarnings("unchecked")
        final DynamoDbIndex<DynamoDbActiveConsentWithExpiryTime> index = mock(DynamoDbIndex.class);
        when(index.query(any(QueryEnhancedRequest.class))).thenReturn(queryResults);
        when(consentTable.index(DynamoDbActiveConsentWithExpiryTime.ACTIVE_CONSENTS_BY_EXPIRY_HOUR_GSI_NAME)).thenReturn(index);
    }
}
