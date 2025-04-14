package com.consentframework.consentexpiryprocessor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import org.junit.jupiter.api.Test;

class ConsentExpiryProcessorTest {
    @Test
    void handleRequest() {
        final Context mockContext = mock(Context.class);
        when(mockContext.getAwsRequestId()).thenReturn("test-request-id");

        final ConsentExpiryProcessor processor = new ConsentExpiryProcessor();
        processor.handleRequest(mockContext);
    }
}
