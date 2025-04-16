package com.consentframework.consentexpiryprocessor.infrastructure.repositories;

import com.consentframework.consentexpiryprocessor.domain.entities.ActiveConsentWithExpiryTime;
import com.consentframework.consentexpiryprocessor.domain.repositories.ConsentRepository;
import com.consentframework.consentexpiryprocessor.infrastructure.mappers.DynamoDbExpiryHourTokenMapper;
import com.consentframework.shared.api.domain.pagination.ListPage;
import com.consentframework.shared.api.infrastructure.entities.DynamoDbActiveConsentWithExpiryTime;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DynamoDB implementation of the consent repository.
 */
public class DynamoDbConsentRepository implements ConsentRepository {
    private final DynamoDbTable<DynamoDbActiveConsentWithExpiryTime> consentTable;

    /**
     * Initializes a new DynamoDB consent repository.
     *
     * @param consentTable The DynamoDB table to use for storing consents.
     */
    public DynamoDbConsentRepository(final DynamoDbTable<DynamoDbActiveConsentWithExpiryTime> consentTable) {
        this.consentTable = consentTable;
    }

    /**
     * Retrieves a paginated list of active consents with non-null expiry times.
     */
    @Override
    public ListPage<ActiveConsentWithExpiryTime> getActiveConsentsWithExpiryHour(final String expiryHour,
            final Optional<String> pageToken) {
        final QueryEnhancedRequest queryRequest = buildGetConsentsToExpireQueryRequest(expiryHour, pageToken);

        final SdkIterable<Page<DynamoDbActiveConsentWithExpiryTime>> queryResults = consentTable
            .index(DynamoDbActiveConsentWithExpiryTime.ACTIVE_CONSENTS_BY_EXPIRY_HOUR_GSI_NAME)
            .query(queryRequest);
        if (queryResults == null) {
            return null;
        }

        return queryResults.stream()
            .findFirst()
            .map(pageResults -> {
                final Optional<String> nextPageToken = getNextPageToken(pageResults);
                final List<ActiveConsentWithExpiryTime> consents = pageResults.items()
                    .stream()
                    .map(ddbActiveConsentGsiItem -> ActiveConsentWithExpiryTime.builder()
                        .id(ddbActiveConsentGsiItem.id())
                        .consentVersion(ddbActiveConsentGsiItem.consentVersion())
                        .expiryHour(ddbActiveConsentGsiItem.expiryHour())
                        .expiryTimeId(ddbActiveConsentGsiItem.expiryTimeId())
                        .build()
                    )
                    .toList();
                return new ListPage<ActiveConsentWithExpiryTime>(consents, nextPageToken);
            })
            .orElse(null);
    }

    /**
     * Updates the status of a consent to expired.
     *
     * TODO: Implement logic to submit a DynamoDB UpdateItem API request to:
     * 1. Update the consent status to "EXPIRED".
     * 2. Remove the consent's autoExpireId attribute.
     * 3. Increment consentVersion by 1.
     */
    @Override
    public void expireConsent(final String id, final String updatedVersion) {
        throw new UnsupportedOperationException("Unimplemented method 'expireConsent'");
    }

    private QueryEnhancedRequest buildGetConsentsToExpireQueryRequest(final String expiryHour, final Optional<String> pageToken) {
        final Map<String, AttributeValue> exclusiveStartKey = DynamoDbExpiryHourTokenMapper.toDynamoDbPageToken(pageToken);

        final QueryConditional queryCondition = QueryConditional.keyEqualTo(Key.builder()
            .partitionValue(expiryHour)
            .build());
        return QueryEnhancedRequest.builder()
            .queryConditional(queryCondition)
            .exclusiveStartKey(exclusiveStartKey)
            .build();
    }

    private Optional<String> getNextPageToken(final Page<DynamoDbActiveConsentWithExpiryTime> pageResults) {
        final Map<String, AttributeValue> lastEvaluatedKey = pageResults.lastEvaluatedKey();
        return DynamoDbExpiryHourTokenMapper.toJsonString(lastEvaluatedKey);
    }
}
