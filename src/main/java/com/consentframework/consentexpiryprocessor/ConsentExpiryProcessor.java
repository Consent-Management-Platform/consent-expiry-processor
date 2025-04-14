package com.consentframework.consentexpiryprocessor;

import com.amazonaws.services.lambda.runtime.Context;
import com.consentframework.consentexpiryprocessor.domain.repositories.ConsentRepository;
import com.consentframework.consentexpiryprocessor.infrastructure.repositories.InMemoryConsentRepository;
import com.consentframework.consentexpiryprocessor.usecases.activities.AutoExpireConsentsActivity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
        this.consentRepository = new InMemoryConsentRepository();
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
