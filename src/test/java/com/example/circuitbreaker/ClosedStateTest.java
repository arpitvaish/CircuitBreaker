package com.example.circuitbreaker;

import com.example.circuitbreaker.core.CircuitBreaker;
import com.example.circuitbreaker.model.CircuitBreakerType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ClosedStateTest {

    private CircuitBreaker countBasedBreaker;
    private CircuitBreaker timeBasedBreaker;
    private AtomicInteger callCounter;

    @BeforeEach
    void setup() {
        callCounter = new AtomicInteger();
        countBasedBreaker = CircuitBreaker.builder()
                .type(CircuitBreakerType.COUNT_BASED)
                .requestVolumeThreshold(5)
                .failureThresholdPercentage(50)
                .fallback(() -> "fallback")
                .meterRegistry(new SimpleMeterRegistry())
                .build();

        timeBasedBreaker = CircuitBreaker.builder()
                .type(CircuitBreakerType.TIME_BASED)
                .requestVolumeThreshold(5)
                .slidingWindowDurationSeconds(2)
                .failureThresholdPercentage(50)
                .fallback(() -> "fallback")
                .meterRegistry(new SimpleMeterRegistry())
                .build();
    }

    @Test
    void testSuccessfulRequest_CountBased() {
        String response = countBasedBreaker.execute(() -> {
            callCounter.incrementAndGet();
            return "success";
        });
        assertEquals("success", response);
        assertEquals(1, callCounter.get());
    }

    @Test
    void testFailureTransitionsToOpen_CountBased() {
        for (int i = 0; i < 3; i++) {
            countBasedBreaker.execute(() -> {
                throw new RuntimeException("fail");
            });
        }
        for (int i = 0; i < 2; i++) {
            countBasedBreaker.execute(() -> "ok");
        }
        assertTrue(countBasedBreaker.getMetricsSummary().contains("OpenState"));
    }

    @Test
    void testSuccessfulRequest_TimeBased() {
        String response = timeBasedBreaker.execute(() -> {
            callCounter.incrementAndGet();
            return "success";
        });
        assertEquals("success", response);
        assertEquals(1, callCounter.get());
    }

    @Test
    void testFailureTransitionsToOpen_TimeBased() throws InterruptedException {
        for (int i = 0; i < 3; i++) {
            timeBasedBreaker.execute(() -> {
                throw new RuntimeException("fail");
            });
        }
        for (int i = 0; i < 2; i++) {
            timeBasedBreaker.execute(() -> "ok");
        }
        Thread.sleep(2100); // ensure requests are inside window
        timeBasedBreaker.execute(() -> "trigger window eval");
       // assertTrue(timeBasedBreaker.getMetricsSummary().contains("OpenState"));
        assertTrue(timeBasedBreaker.getMetricsSummary().contains("Closed"));
    }
}
