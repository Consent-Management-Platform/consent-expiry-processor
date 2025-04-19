package com.consentframework.consentexpiryprocessor.infrastructure.repositories;

import com.consentframework.consentexpiryprocessor.domain.entities.ActiveConsentWithExpiryTime;
import com.consentframework.consentexpiryprocessor.domain.repositories.ConsentRepository;
import com.consentframework.consentexpiryprocessor.infrastructure.mappers.DynamoDbExpiryHourTokenMapper;
import com.consentframework.consentexpiryprocessor.infrastructure.metrics.CloudWatchMetricsHandler;
import com.consentframework.shared.api.domain.pagination.ListPage;
import com.consentframework.shared.api.infrastructure.entities.DynamoDbActiveConsentWithExpiryTime;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DynamoDB implementation of the consent repository.
 */
public class DynamoDbConsentRepository implements ConsentRepository {
    private static final Logger logger = LogManager.getLogger(DynamoDbConsentRepository.class);

    private static final String EXPIRED_STATUS = "EXPIRED";

    private final DynamoDbClient ddbClient;
    private final DynamoDbTable<DynamoDbActiveConsentWithExpiryTime> consentTable;
    private final CloudWatchMetricsHandler metricsHandler;

    /**
     * Initializes a new DynamoDB consent repository.
     *
     * @param ddbClient The DynamoDB client used to update items.
     * @param consentTable The DynamoDB table used to query active consents with expiry times.
     */
    public DynamoDbConsentRepository(final DynamoDbClient ddbClient,
            final DynamoDbTable<DynamoDbActiveConsentWithExpiryTime> consentTable,
            final CloudWatchMetricsHandler metricsHandler) {
        this.ddbClient = ddbClient;
        this.consentTable = consentTable;
        this.metricsHandler = metricsHandler;
    }

    /**
     * Retrieves a paginated list of active consents with non-null expiry times.
     */
    @Override
    public ListPage<ActiveConsentWithExpiryTime> getActiveConsentsWithExpiryHour(final String expiryHour,
            final Optional<String> pageToken) {
        final String context = String.format("expiry hour %s, pageToken %s", expiryHour, pageToken.orElse("null"));
        logger.info("Retrieving active consents with {}", context);
        final QueryEnhancedRequest queryRequest = buildGetConsentsToExpireQueryRequest(expiryHour, pageToken);

        final SdkIterable<Page<DynamoDbActiveConsentWithExpiryTime>> queryResults = consentTable
            .index(DynamoDbActiveConsentWithExpiryTime.ACTIVE_CONSENTS_BY_EXPIRY_HOUR_GSI_NAME)
            .query(queryRequest);
        if (queryResults == null) {
            logger.info("Received null query results for {}", context);
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
                logger.info("Received page of {} consents with nextPageToken {} when querying {}",
                    consents.size(), nextPageToken, context);
                return new ListPage<ActiveConsentWithExpiryTime>(consents, nextPageToken);
            })
            .orElse(null);
    }

    /**
     * Updates the status of a consent to expired.
     */
    @Override
    public void expireConsent(final String id, final String updatedVersion) {
        logger.info("Updating consent with id {} to expired, with updated version {}", id, updatedVersion);
        final UpdateItemRequest expireConsentRequest = buildExpireConsentRequest(id, updatedVersion);
        this.ddbClient.updateItem(expireConsentRequest);
        logger.info("Successfully expired consent with id {}", id);
    }

    private UpdateItemRequest buildExpireConsentRequest(final String id, final String updatedVersion) {
        final Integer updatedVersionInt = Integer.parseInt(updatedVersion);
        final Integer lastVersionInt = updatedVersionInt - 1;

        // Use REMOVE to remove GSI key attributes, and SET to update other attributes.
        // Ref: https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html#Expressions.UpdateExpressions.Multiple
        final String updateExpression = "SET #consentStatus = :expiredStatus, "
            + "#consentVersion = :nextConsentVersion "
            + "REMOVE #expiryHour, #expiryTimeId";

        final String conditionExpression = "attribute_exists(id) AND #consentVersion = :expectedLastVersion";

        final Map<String, AttributeValue> key = Map.of(
            "id", AttributeValue.builder().s(id).build()
        );
        final Map<String, String> expressionAttributeNames = Map.of(
            "#consentStatus", "consentStatus",
            "#consentVersion", "consentVersion",
            "#expiryHour", "expiryHour",
            "#expiryTimeId", "expiryTimeId"
        );
        final Map<String, AttributeValue> expressionAttributeValues = Map.of(
            ":expectedLastVersion", AttributeValue.builder().n(lastVersionInt.toString()).build(),
            ":expiredStatus", AttributeValue.builder().s(EXPIRED_STATUS).build(),
            ":nextConsentVersion", AttributeValue.builder().n(updatedVersion).build()
        );

        return UpdateItemRequest.builder()
            .tableName(DynamoDbActiveConsentWithExpiryTime.TABLE_NAME)
            .key(key)
            .updateExpression(updateExpression)
            .conditionExpression(conditionExpression)
            .expressionAttributeNames(expressionAttributeNames)
            .expressionAttributeValues(expressionAttributeValues)
            .build();
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
