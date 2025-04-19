package com.consentframework.consentexpiryprocessor;

import com.amazonaws.services.lambda.runtime.Context;
import com.consentframework.consentexpiryprocessor.domain.repositories.ConsentRepository;
import com.consentframework.consentexpiryprocessor.infrastructure.metrics.CloudWatchMetricsHandler;
import com.consentframework.consentexpiryprocessor.infrastructure.repositories.DynamoDbConsentRepository;
import com.consentframework.consentexpiryprocessor.usecases.activities.AutoExpireConsentsActivity;
import com.consentframework.shared.api.infrastructure.entities.DynamoDbActiveConsentWithExpiryTime;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Entry point for the application, orchestrates job execution to auto-expire consents.
 */
public class ConsentExpiryProcessor {
    public static final String EXPIRY_JOB_FAILURE_METRIC_NAME = "ConsentExpiryJobFailure";

    private static final Logger logger = LogManager.getLogger(ConsentExpiryProcessor.class);

    final ConsentRepository consentRepository;
    final AutoExpireConsentsActivity autoExpireConsentsActivity;
    final CloudWatchMetricsHandler metricsHandler;

    /**
     * Creates a new instance of ConsentExpiryProcessor.
     */
    public ConsentExpiryProcessor() {
        final DynamoDbClient ddbClient = DynamoDbClient.builder()
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
        final DynamoDbEnhancedClient ddbEnhancedClient = DynamoDbEnhancedClient.builder()
            .dynamoDbClient(ddbClient)
            .build();
        final DynamoDbTable<DynamoDbActiveConsentWithExpiryTime> consentTable = ddbEnhancedClient.table(
            DynamoDbActiveConsentWithExpiryTime.TABLE_NAME,
            TableSchema.fromClass(DynamoDbActiveConsentWithExpiryTime.class));

        final CloudWatchClient cloudWatchClient = CloudWatchClient.create();
        this.metricsHandler = new CloudWatchMetricsHandler(cloudWatchClient);
        this.consentRepository = new DynamoDbConsentRepository(ddbClient, consentTable, metricsHandler);
        this.autoExpireConsentsActivity = new AutoExpireConsentsActivity(consentRepository);
    }

    /**
     * Instantiates the ConsentExpiryProcessor with a given consent repository.
     *
     * @param consentRepository The consent repository.
     */
    public ConsentExpiryProcessor(final ConsentRepository consentRepository, final CloudWatchClient cloudWatchClient) {
        this.consentRepository = consentRepository;
        this.autoExpireConsentsActivity = new AutoExpireConsentsActivity(consentRepository);
        this.metricsHandler = new CloudWatchMetricsHandler(cloudWatchClient);
    }

    /**
     * Handle the Lambda request.
     *
     * @param context The Lambda context.
     */
    public void handleRequest(final Context context) {
        logger.info("Handling request to update status of consents past their expiry time, request ID: {}.", context.getAwsRequestId());
        executeAutoExpireConsentsActivity(context);

        logger.info("Successfully processed auto expire consents request, request ID: {}.", context.getAwsRequestId());
        metricsHandler.publishCountMetric(EXPIRY_JOB_FAILURE_METRIC_NAME, 0);
    }

    private void executeAutoExpireConsentsActivity(final Context context) {
        try {
            autoExpireConsentsActivity.execute();
        } catch (final Exception e) {
            logger.error("Failed to process auto expire consents request, request ID: {}.", context.getAwsRequestId());
            metricsHandler.publishCountMetric(EXPIRY_JOB_FAILURE_METRIC_NAME, 1);
            throw e;
        }
    }
}
