package com.consentframework.consentexpiryprocessor.infrastructure.mappers;

import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.Optional;

/**
 * Utility methods for converting ActiveConsentsByExpiryHour GSI page tokens between data models.
 */
public final class DynamoDbExpiryHourTokenMapper {
    private DynamoDbExpiryHourTokenMapper() {}

    /**
     * Converts the JSON string to a DynamoDB page token that can be used in DynamoDB API queries.
     *
     * @param nextPageToken JSON string representation of the next page token.
     * @return DynamoDB AttributeValue map representation of the next page token.
     */
    public static Map<String, AttributeValue> toDynamoDbPageToken(final Optional<String> nextPageToken) {
        if (nextPageToken.isPresent()) {
            return EnhancedDocument.fromJson(nextPageToken.get()).toMap();
        }
        return null;
    }

    /**
     * Converts the DynamoDB page token to a JSON string.
     *
     * @param pageToken DynamoDB AttributeValue map representation of the next page token.
     * @return JSON string representation of the next page token.
     */
    public static Optional<String> toJsonString(final Map<String, AttributeValue> pageToken) {
        if (pageToken != null) {
            return Optional.of(EnhancedDocument.fromAttributeValueMap(pageToken).toJson());
        }
        return Optional.empty();
    }
}
