package com.example.circuitbreaker.core;

import java.util.ArrayDeque;
import java.util.Deque;

// com.example.circuitbreaker.core.TimeBasedEvaluationStrategy.java
public class TimeBasedEvaluationStrategy implements EvaluationStrategy {
    private final Deque<CallRecord> window = new ArrayDeque<>();
    private final int windowSeconds;
    private final int failureThreshold;

    public TimeBasedEvaluationStrategy(int thresholdPercentage, int windowSeconds) {
        this.failureThreshold = thresholdPercentage;
        this.windowSeconds = windowSeconds;
    }

    @Override
    public void recordSuccess() {
        window.add(new CallRecord(false));
        cleanOldRecords();
    }

    @Override
    public void recordFailure() {
        window.add(new CallRecord(true));
        cleanOldRecords();
    }

    @Override
    public boolean shouldTripOpen() {
        cleanOldRecords();
        if (window.isEmpty()) return false;

        long failures = window.stream().filter(CallRecord::isFailure).count();
        int rate = (int) ((failures * 100) / window.size());
        return rate >= failureThreshold;
    }

    @Override
    public void reset() {
        window.clear();
    }

    private void cleanOldRecords() {
        long cutoff = System.currentTimeMillis() - (windowSeconds * 1000L);
        while (!window.isEmpty() && window.peek().timestamp < cutoff) {
            window.poll();
        }
    }

    private static class CallRecord {
        long timestamp;
        boolean failure;

        CallRecord(boolean failure) {
            this.timestamp = System.currentTimeMillis();
            this.failure = failure;
        }

        boolean isFailure() {
            return failure;
        }
    }
}

