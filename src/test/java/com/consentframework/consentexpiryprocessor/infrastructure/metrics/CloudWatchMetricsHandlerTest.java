package com.consentframework.consentexpiryprocessor.infrastructure.metrics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.consentframework.consentexpiryprocessor.testcommon.matchers.PutMetricDataRequestMatcher;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.CloudWatchException;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;

class CloudWatchMetricsHandlerTest {
    @Test
    void testPublishCountMetric() {
        final CloudWatchClient cloudWatchClient = mock(CloudWatchClient.class);
        when(cloudWatchClient.putMetricData(any(PutMetricDataRequest.class))).thenReturn(null);
        final CloudWatchMetricsHandler metricsHandler = new CloudWatchMetricsHandler(cloudWatchClient);

        final String metricName = "TestMetricName";
        final Integer metricValue = 15;
        metricsHandler.publishCountMetric(metricName, metricValue);

        verify(cloudWatchClient).putMetricData(argThat(new PutMetricDataRequestMatcher(
            CloudWatchMetricsHandler.METRIC_NAMESPACE,
            metricName,
            metricValue.doubleValue()
        )));
    }

    @Test
    void testPublishCountMetricHandlesCloudWatchError() {
        final CloudWatchClient cloudWatchClient = mock(CloudWatchClient.class);
        when(cloudWatchClient.putMetricData(any(PutMetricDataRequest.class)))
            .thenThrow(CloudWatchException.builder().message("Test CloudWatch error").build());
        final CloudWatchMetricsHandler metricsHandler = new CloudWatchMetricsHandler(cloudWatchClient);

        final String metricName = "TestMetricName";
        final Integer metricValue = 15;
        metricsHandler.publishCountMetric(metricName, metricValue);

        verify(cloudWatchClient).putMetricData(argThat(new PutMetricDataRequestMatcher(
            CloudWatchMetricsHandler.METRIC_NAMESPACE,
            metricName,
            metricValue.doubleValue()
        )));
    }
}
