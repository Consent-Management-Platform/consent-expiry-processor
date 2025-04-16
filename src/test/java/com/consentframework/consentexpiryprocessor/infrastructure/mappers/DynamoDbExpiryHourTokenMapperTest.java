package com.consentframework.consentexpiryprocessor.infrastructure.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.Optional;

class DynamoDbExpiryHourTokenMapperTest {
    private static final String PARTITION_KEY_NAME = "testPartitionKey";
    private static final String PARTITION_KEY_VALUE = "partitionKeyValue";
    private static final String SORT_KEY_NAME = "testSortKey";
    private static final String SORT_KEY_VALUE = "sortKeyValue";
    private static final String NEXT_TOKEN_JSON_STRING = String.format(
        "{\"%s\":\"%s\",\"%s\":\"%s\"}",
        PARTITION_KEY_NAME, PARTITION_KEY_VALUE, SORT_KEY_NAME, SORT_KEY_VALUE);
    private static final Map<String, AttributeValue> NEXT_TOKEN_ATTRIBUTE_VALUE_MAP = Map.of(
        PARTITION_KEY_NAME, AttributeValue.builder().s(PARTITION_KEY_VALUE).build(),
        SORT_KEY_NAME, AttributeValue.builder().s(SORT_KEY_VALUE).build()
    );

    @Test
    void toDynamoDbPageTokenWhenEmpty() {
        final Optional<String> nextPageTokenInput = Optional.empty();
        final Map<String, AttributeValue> pageTokenMap = DynamoDbExpiryHourTokenMapper.toDynamoDbPageToken(nextPageTokenInput);
        assertNull(pageTokenMap);
    }

    @Test
    void toDynamoDbPageTokenWhenPresent() {
        final Map<String, AttributeValue> pageTokenMap = DynamoDbExpiryHourTokenMapper.toDynamoDbPageToken(
            Optional.of(NEXT_TOKEN_JSON_STRING));
        assertNotNull(pageTokenMap);
        assertEquals(PARTITION_KEY_VALUE, pageTokenMap.get(PARTITION_KEY_NAME).s());
        assertEquals(SORT_KEY_VALUE, pageTokenMap.get(SORT_KEY_NAME).s());
    }

    @Test
    void toJsonStringWhenNull() {
        final Optional<String> parsedJsonString = DynamoDbExpiryHourTokenMapper.toJsonString(null);
        assertTrue(parsedJsonString.isEmpty());
    }

    @Test
    void toJsonStringWhenPresent() throws Exception {
        final Optional<String> parsedJsonString = DynamoDbExpiryHourTokenMapper.toJsonString(NEXT_TOKEN_ATTRIBUTE_VALUE_MAP);
        assertTrue(parsedJsonString.isPresent());

        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode expectedJson = objectMapper.readTree(NEXT_TOKEN_JSON_STRING);
        final JsonNode parsedJson = objectMapper.readTree(parsedJsonString.get());
        assertEquals(expectedJson, parsedJson);
    }
}
