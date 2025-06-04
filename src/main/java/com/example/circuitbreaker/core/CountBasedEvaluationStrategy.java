package com.example.circuitbreaker.core;

// com.example.circuitbreaker.core.CountBasedEvaluationStrategy.java
public class CountBasedEvaluationStrategy implements EvaluationStrategy {
    private int failureCount = 0;
    private int successCount = 0;
    private final int threshold;
    private final int requestVolumeThreshold;

    public CountBasedEvaluationStrategy(int thresholdPercentage, int requestVolumeThreshold) {
        this.threshold = thresholdPercentage;
        this.requestVolumeThreshold = requestVolumeThreshold;
    }

    @Override
    public void recordSuccess() {
        successCount++;
    }

    @Override
    public void recordFailure() {
        failureCount++;
    }

    @Override
    public boolean shouldTripOpen() {
        int total = failureCount + successCount;
        if (total < requestVolumeThreshold) return false;
        int failureRate = (failureCount * 100) / total;
        return failureRate >= threshold;
    }

    @Override
    public void reset() {
        failureCount = 0;
        successCount = 0;
    }
}
