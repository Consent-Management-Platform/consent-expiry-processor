package com.consentframework.consentexpiryprocessor;

import com.amazonaws.services.lambda.runtime.Context;
import com.consentframework.consentexpiryprocessor.domain.repositories.ConsentRepository;
import com.consentframework.consentexpiryprocessor.infrastructure.repositories.DynamoDbConsentRepository;
import com.consentframework.consentexpiryprocessor.usecases.activities.AutoExpireConsentsActivity;
import com.consentframework.shared.api.infrastructure.entities.DynamoDbActiveConsentWithExpiryTime;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Entry point for the application, orchestrates job execution to auto-expire consents.
 */
public class ConsentExpiryProcessor {
    private static final Logger logger = LogManager.getLogger(ConsentExpiryProcessor.class);

    final ConsentRepository consentRepository;
    final AutoExpireConsentsActivity autoExpireConsentsActivity;

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

        this.consentRepository = new DynamoDbConsentRepository(ddbClient, consentTable);
        this.autoExpireConsentsActivity = new AutoExpireConsentsActivity(consentRepository);
    }

    /**
     * Instantiates the ConsentExpiryProcessor with a given consent repository.
     *
     * @param consentRepository The consent repository.
     */
    public ConsentExpiryProcessor(final ConsentRepository consentRepository) {
        this.consentRepository = consentRepository;
        this.autoExpireConsentsActivity = new AutoExpireConsentsActivity(consentRepository);
    }

    /**
     * Handle the Lambda request.
     *
     * @param context The Lambda context.
     */
    public void handleRequest(final Context context) {
        logger.info("Handling request to update status of consents past their expiry time, request ID: {}.", context.getAwsRequestId());
        autoExpireConsentsActivity.execute();
        logger.info("Successfully processed auto expire consents request, request ID: {}.", context.getAwsRequestId());
    }
}
