package com.consentframework.consentexpiryprocessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.consentframework.consentexpiryprocessor.domain.repositories.ConsentRepository;
import com.consentframework.consentexpiryprocessor.infrastructure.metrics.CloudWatchMetricsHandler;
import com.consentframework.consentexpiryprocessor.infrastructure.repositories.InMemoryConsentRepository;
import com.consentframework.consentexpiryprocessor.testcommon.matchers.PutMetricDataRequestMatcher;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;

class ConsentExpiryProcessorTest {
    @Test
    void handleRequestWhenSuccess() {
        final Context mockContext = mock(Context.class);
        when(mockContext.getAwsRequestId()).thenReturn("test-request-id");

        final CloudWatchClient cloudWatchClient = mock(CloudWatchClient.class);
        when(cloudWatchClient.putMetricData(any(PutMetricDataRequest.class))).thenReturn(null);

        final ConsentRepository consentRepository = new InMemoryConsentRepository();
        final ConsentExpiryProcessor processor = new ConsentExpiryProcessor(consentRepository, cloudWatchClient);
        processor.handleRequest(mockContext);

        verify(cloudWatchClient).putMetricData(argThat(new PutMetricDataRequestMatcher(
            CloudWatchMetricsHandler.METRIC_NAMESPACE,
            ConsentExpiryProcessor.EXPIRY_JOB_FAILURE_METRIC_NAME,
            0.0
        )));
    }

    @Test
    void handleRequestWhenFailure() {
        final Context mockContext = mock(Context.class);
        when(mockContext.getAwsRequestId()).thenReturn("test-request-id");

        final CloudWatchClient cloudWatchClient = mock(CloudWatchClient.class);
        when(cloudWatchClient.putMetricData(any(PutMetricDataRequest.class))).thenReturn(null);

        final Exception testException = new RuntimeException("Test error message");
        final ConsentRepository consentRepository = spy(new InMemoryConsentRepository());
        when(consentRepository.getActiveConsentsWithExpiryHour(any(), any())).thenThrow(testException);

        final ConsentExpiryProcessor processor = new ConsentExpiryProcessor(consentRepository, cloudWatchClient);
        final RuntimeException thrownException = assertThrows(RuntimeException.class, () -> processor.handleRequest(mockContext));
        assertEquals(testException, thrownException);

        verify(cloudWatchClient).putMetricData(argThat(new PutMetricDataRequestMatcher(
            CloudWatchMetricsHandler.METRIC_NAMESPACE,
            ConsentExpiryProcessor.EXPIRY_JOB_FAILURE_METRIC_NAME,
            1.0
        )));
    }
}
