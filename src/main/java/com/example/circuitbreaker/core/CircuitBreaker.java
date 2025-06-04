package com.example.circuitbreaker.core;

import com.example.circuitbreaker.core.state.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.coyote.RequestInfo;

import java.time.Instant;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;



import com.example.circuitbreaker.model.CircuitBreakerType;
import com.example.circuitbreaker.core.state.*;

/**
 * Core Circuit Breaker class that delegates behavior to its current state.
 * Supports count-based and time-based sliding windows.
 */
public class CircuitBreaker {

    private final CircuitBreakerType type;
    private final int failureThresholdPercentage;
    private final int requestVolumeThreshold;
    private final int slidingWindowDurationSeconds;
    private final long openStateTimeoutMillis;
    private final Supplier<String> fallback;
    private final BiConsumer<String, String> onStateChange;

    private State currentState;
    private final Queue<RequestInfo> requestLog = new LinkedList<>();
    private final MeterRegistry meterRegistry;
    private final Counter stateChangeCounter;
    private final Counter blockedRequestCounter;
    private final Counter failureCounter;

    private Instant openStateStartTime;

    private CircuitBreaker(Builder builder) {
        this.type = builder.type;
        this.failureThresholdPercentage = builder.failureThresholdPercentage;
        this.requestVolumeThreshold = builder.requestVolumeThreshold;
        this.slidingWindowDurationSeconds = builder.slidingWindowDurationSeconds;
        this.openStateTimeoutMillis = builder.openStateTimeoutMillis;
        this.fallback = builder.fallback;
        this.onStateChange = builder.onStateChange;
        this.meterRegistry = builder.meterRegistry;
        this.stateChangeCounter = meterRegistry.counter("circuitbreaker_statechange_total");
        this.blockedRequestCounter = meterRegistry.counter("circuitbreaker_blocked_total");
        this.failureCounter = meterRegistry.counter("circuitbreaker_failures_total");
        this.currentState = new ClosedState(this);
    }

    public String execute(Supplier<String> remoteCall) {
        return currentState.handle(remoteCall);
    }

    public void recordRequest(boolean success, long latencyMillis) {
        synchronized (requestLog) {
            requestLog.add(new RequestInfo(success, latencyMillis, Instant.now()));
            while (requestLog.size() > requestVolumeThreshold ||
                    (type == CircuitBreakerType.TIME_BASED &&
                            !requestLog.isEmpty() &&
                            requestLog.peek().timestamp.isBefore(Instant.now().minusSeconds(slidingWindowDurationSeconds)))) {
                requestLog.poll();
            }
        }
    }

    public double calculateFailureRate() {
        synchronized (requestLog) {
            if (requestLog.isEmpty()) return 0.0;
            long failures = requestLog.stream().filter(r -> !r.success || r.latencyMillis > 500).count();
            return (100.0 * failures) / requestLog.size();
        }
    }

    public void transitionTo(State newState) {
        String old = currentState.getClass().getSimpleName();
        String next = newState.getClass().getSimpleName();
        this.currentState = newState;
        stateChangeCounter.increment();
        if (onStateChange != null) onStateChange.accept(old, next);
        if (newState instanceof OpenState) {
            openStateStartTime = Instant.now();
        }
    }

    public boolean isOpenStateTimedOut() {
        return openStateStartTime != null &&
                Instant.now().isAfter(openStateStartTime.plusMillis(openStateTimeoutMillis));
    }

    public String getFallback() {
        return fallback.get();
    }

    public void incrementBlockedRequest() {
        blockedRequestCounter.increment();
    }

    public void incrementFailure() {
        failureCounter.increment();
    }

    public String getMetricsSummary() {
        return String.format("State: %s, Failure Rate: %.2f%%, Requests: %d",
                currentState.getClass().getSimpleName(),
                calculateFailureRate(),
                requestLog.size());
    }

    /**
     * Builder class for creating a CircuitBreaker instance with fluent API.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private CircuitBreakerType type = CircuitBreakerType.COUNT_BASED;
        private int failureThresholdPercentage = 50;
        private int requestVolumeThreshold = 5;
        private int slidingWindowDurationSeconds = 10;
        private long openStateTimeoutMillis = 1000;
        private Supplier<String> fallback = () -> "Default fallback";
        private BiConsumer<String, String> onStateChange = null;
        private MeterRegistry meterRegistry;

        public Builder type(CircuitBreakerType type) {
            this.type = type;
            return this;
        }

        public Builder failureThresholdPercentage(int failureThresholdPercentage) {
            this.failureThresholdPercentage = failureThresholdPercentage;
            return this;
        }

        public Builder requestVolumeThreshold(int threshold) {
            this.requestVolumeThreshold = threshold;
            return this;
        }

        public Builder slidingWindowDurationSeconds(int seconds) {
            this.slidingWindowDurationSeconds = seconds;
            return this;
        }

        public Builder openStateTimeoutMillis(long millis) {
            this.openStateTimeoutMillis = millis;
            return this;
        }

        public Builder fallback(Supplier<String> fallback) {
            this.fallback = fallback;
            return this;
        }

        public Builder onStateChange(BiConsumer<String, String> consumer) {
            this.onStateChange = consumer;
            return this;
        }

        public Builder meterRegistry(MeterRegistry registry) {
            this.meterRegistry = registry;
            return this;
        }

        public CircuitBreaker build() {
            return new CircuitBreaker(this);
        }
    }

    private static class RequestInfo {
        boolean success;
        long latencyMillis;
        Instant timestamp;

        RequestInfo(boolean success, long latencyMillis, Instant timestamp) {
            this.success = success;
            this.latencyMillis = latencyMillis;
            this.timestamp = timestamp;
        }
    }

    public CircuitBreakerType getType() {
        return type;
    }

    public int getFailureThresholdPercentage() {
        return failureThresholdPercentage;
    }

    public int getRequestVolumeThreshold() {
        return requestVolumeThreshold;
    }

    public int getSlidingWindowDurationSeconds() {
        return slidingWindowDurationSeconds;
    }
}
