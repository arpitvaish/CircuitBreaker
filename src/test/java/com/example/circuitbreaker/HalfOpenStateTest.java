package com.example.circuitbreaker;


import com.example.circuitbreaker.core.CircuitBreaker;
import com.example.circuitbreaker.model.CircuitBreakerType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HalfOpenStateTest {

    private CircuitBreaker countBasedBreaker;
    private CircuitBreaker timeBasedBreaker;

    @BeforeEach
    void setup() throws InterruptedException {
        countBasedBreaker = CircuitBreaker.builder()
                .type(CircuitBreakerType.COUNT_BASED)
                .requestVolumeThreshold(2)
                .failureThresholdPercentage(50)
                .openStateTimeoutMillis(1000)
                .fallback(() -> "fallback")
                .meterRegistry(new SimpleMeterRegistry())
                .build();

        timeBasedBreaker = CircuitBreaker.builder()
                .type(CircuitBreakerType.TIME_BASED)
                .requestVolumeThreshold(2)
                .slidingWindowDurationSeconds(1)
                .failureThresholdPercentage(50)
                .openStateTimeoutMillis(1000)
                .fallback(() -> "fallback")
                .meterRegistry(new SimpleMeterRegistry())
                .build();

        // Force both into OPEN
        for (int i = 0; i < 2; i++) {
            countBasedBreaker.execute(() -> { throw new RuntimeException("fail"); });
            timeBasedBreaker.execute(() -> { throw new RuntimeException("fail"); });
        }

        Thread.sleep(1100); // move to HALF_OPEN state
    }

    @Test
    void testHalfOpenAllPassTransitionToClosed_CountBased() {
        for (int i = 0; i < 2; i++) {
            String result = countBasedBreaker.execute(() -> "recovery");
            assertEquals("recovery", result);
        }
        assertTrue(countBasedBreaker.getMetricsSummary().contains("ClosedState"));
    }

    @Test
    void testHalfOpenOneFailTransitionToOpen_CountBased() {
        countBasedBreaker.execute(() -> "ok");
        countBasedBreaker.execute(() -> { throw new RuntimeException("fail"); });
        assertTrue(countBasedBreaker.getMetricsSummary().contains("OpenState"));
    }

    @Test
    void testHalfOpenAllPassTransitionToClosed_TimeBased() {
        for (int i = 0; i < 2; i++) {
            String result = timeBasedBreaker.execute(() -> "recovery");
            assertEquals("recovery", result);
        }
        assertTrue(timeBasedBreaker.getMetricsSummary().contains("ClosedState"));
    }

    @Test
    void testHalfOpenOneFailTransitionToOpen_TimeBased() {
        timeBasedBreaker.execute(() -> "ok");
        timeBasedBreaker.execute(() -> { throw new RuntimeException("fail"); });
        assertTrue(timeBasedBreaker.getMetricsSummary().contains("OpenState"));
    }
}