package com.consentframework.consentexpiryprocessor.testcommon.matchers;

import org.mockito.ArgumentMatcher;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

/**
 * A matcher for {@link PutMetricDataRequest} that validates that non-timestamp attributes match the input.
 */
public class PutMetricDataRequestMatcher implements ArgumentMatcher<PutMetricDataRequest> {
    private final String metricNamespace;
    private final String metricName;
    private final Double metricValue;

    /**
     * Initialize matcher with metric name and value.
     */
    public PutMetricDataRequestMatcher(final String metricNamespace, final String metricName, final Double metricValue) {
        this.metricNamespace = metricNamespace;
        this.metricName = metricName;
        this.metricValue = metricValue;
    }

    @Override
    public boolean matches(final PutMetricDataRequest argument) {
        if (argument == null) {
            return false;
        }
        if (!metricNamespace.equals(argument.namespace())) {
            return false;
        }
        if (argument.metricData().size() != 1) {
            return false;
        }
        final MetricDatum datum = argument.metricData().get(0);
        if (!metricName.equals(datum.metricName())) {
            return false;
        }
        if (!StandardUnit.COUNT.equals(datum.unit())) {
            return false;
        }
        if (!metricValue.equals(datum.value())) {
            return false;
        }
        return true;
    }
}
