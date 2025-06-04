package com.example.circuitbreaker.core.state;

import com.example.circuitbreaker.core.CircuitBreaker;


import com.example.circuitbreaker.core.CircuitBreaker;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * HALF-OPEN state: Allows limited requests to test service recovery.
 */
public class HalfOpenState implements State {
    private final CircuitBreaker breaker;
    private final AtomicInteger allowedTrial = new AtomicInteger(3);

    public HalfOpenState(CircuitBreaker breaker) {
        this.breaker = breaker;
    }

    @Override
    public String handle(Supplier<String> remoteCall) {
        if (allowedTrial.decrementAndGet() < 0) {
            breaker.transitionTo(new OpenState(breaker));
            return breaker.getFallback();
        }

        long start = System.currentTimeMillis();
        try {
            String result = remoteCall.get();
            breaker.recordRequest(true, System.currentTimeMillis() - start);
            if (breaker.calculateFailureRate() < breaker.getFailureThresholdPercentage()) {
                breaker.transitionTo(new ClosedState(breaker));
            } else {
                breaker.transitionTo(new OpenState(breaker));
            }
            return result;
        } catch (Exception ex) {
            breaker.incrementFailure();
            breaker.recordRequest(false, System.currentTimeMillis() - start);
            breaker.transitionTo(new OpenState(breaker));
            return breaker.getFallback();
        }
    }
}
