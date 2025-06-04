package com.example.circuitbreaker;

import com.example.circuitbreaker.core.CircuitBreaker;
import com.example.circuitbreaker.model.CircuitBreakerType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenStateTest {

    private CircuitBreaker countBasedBreaker;
    private CircuitBreaker timeBasedBreaker;

    @BeforeEach
    void setup() {
        countBasedBreaker = CircuitBreaker.builder()
                .type(CircuitBreakerType.COUNT_BASED)
                .requestVolumeThreshold(5)
                .failureThresholdPercentage(50)
                .openStateTimeoutMillis(2000)
                .fallback(() -> "fallback")
                .meterRegistry(new SimpleMeterRegistry())
                .build();

        timeBasedBreaker = CircuitBreaker.builder()
                .type(CircuitBreakerType.TIME_BASED)
                .requestVolumeThreshold(5)
                .slidingWindowDurationSeconds(2)
                .failureThresholdPercentage(50)
                .openStateTimeoutMillis(2000)
                .fallback(() -> "fallback")
                .meterRegistry(new SimpleMeterRegistry())
                .build();

        for (int i = 0; i < 5; i++) {
            countBasedBreaker.execute(() -> {
                throw new RuntimeException("fail");
            });
            timeBasedBreaker.execute(() -> {
                throw new RuntimeException("fail");
            });
        }
    }

    @Test
    void testFallbackResponse_CountBased() {
        String response = countBasedBreaker.execute(() -> "should not be called");
        assertEquals("fallback", response);
    }

    @Test
    void testFallbackResponse_TimeBased() {
        String response = timeBasedBreaker.execute(() -> "should not be called");
        assertEquals("fallback", response);
    }

    @Test
    void testOpenTimeoutToHalfOpen_CountBased() throws InterruptedException {
        Thread.sleep(2100);
        assertTrue(countBasedBreaker.isOpenStateTimedOut());
    }

    @Test
    void testOpenTimeoutToHalfOpen_TimeBased() throws InterruptedException {
        Thread.sleep(2100);
        assertTrue(timeBasedBreaker.isOpenStateTimedOut());
    }
}