package com.consentframework.consentexpiryprocessor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.consentframework.consentexpiryprocessor.domain.repositories.ConsentRepository;
import com.consentframework.consentexpiryprocessor.infrastructure.repositories.InMemoryConsentRepository;
import org.junit.jupiter.api.Test;

class ConsentExpiryProcessorTest {
    @Test
    void handleRequest() {
        final Context mockContext = mock(Context.class);
        when(mockContext.getAwsRequestId()).thenReturn("test-request-id");

        final ConsentRepository consentRepository = new InMemoryConsentRepository();
        final ConsentExpiryProcessor processor = new ConsentExpiryProcessor(consentRepository);
        processor.handleRequest(mockContext);
    }
}
