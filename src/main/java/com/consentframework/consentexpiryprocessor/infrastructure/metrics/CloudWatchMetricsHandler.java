package com.consentframework.consentexpiryprocessor.infrastructure.metrics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.CloudWatchException;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

import java.time.Instant;

/**
 * Encapsulates logic for publishing metrics to AWS CloudWatch.
 */
public class CloudWatchMetricsHandler {
    public static final String METRIC_NAMESPACE = "ConsentExpiryProcessor";

    private static final Logger logger = LogManager.getLogger(CloudWatchMetricsHandler.class);

    private CloudWatchClient cloudWatchClient;

    /**
     * Constructs a new CloudWatchMetricsHandler.
     */
    public CloudWatchMetricsHandler(final CloudWatchClient cloudWatchClient) {
        this.cloudWatchClient = cloudWatchClient;
    }

    /**
     * Publishes a CloudWatch count metric.
     *
     * @param metricName The CloudWatch metric name
     * @param metricValue The value to set in the emitted metric
     */
    public void publishCountMetric(final String metricName, final Integer metricValue) {
        publishMetric(metricName, metricValue.doubleValue());
    }

    /**
     * Publishes a CloudWatch metric.
     *
     * @param metricName The CloudWatch metric name
     * @param metricValue The value to set in the emitted metric
     */
    public void publishMetric(final String metricName, final Double metricValue) {
        final Instant currentTime = Instant.now();
        final MetricDatum metricDatum = MetricDatum.builder()
            .metricName(metricName)
            .unit(StandardUnit.COUNT)
            .value(metricValue)
            .timestamp(currentTime)
            .build();
        final PutMetricDataRequest putMetricDataRequest = PutMetricDataRequest.builder()
            .namespace(METRIC_NAMESPACE)
            .metricData(metricDatum)
            .build();
        try {
            cloudWatchClient.putMetricData(putMetricDataRequest);
        } catch (final CloudWatchException e) {
            final String errorMessage = String.format("Failed to publish CloudWatch metric %s", metricDatum.toString());
            logger.error(errorMessage, e);
        }
    }
}
