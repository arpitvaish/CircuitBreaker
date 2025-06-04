package com.example.circuitbreaker.core.state;

import com.example.circuitbreaker.core.CircuitBreaker;

import java.util.function.Supplier;


/**
 * CLOSED state: All requests are passed through.
 */
public class ClosedState implements State {
    private final CircuitBreaker breaker;

    public ClosedState(CircuitBreaker breaker) {
        this.breaker = breaker;
    }

    @Override
    public String handle(Supplier<String> remoteCall) {
        long start = System.currentTimeMillis();
        try {
            String result = remoteCall.get();
            breaker.recordRequest(true, System.currentTimeMillis() - start);
            double failureRate = breaker.calculateFailureRate();
            if (failureRate > breaker.getFailureThresholdPercentage()) {
                breaker.transitionTo(new OpenState(breaker));
            }
            return result;
        } catch (Exception ex) {
            breaker.incrementFailure();
            breaker.recordRequest(false, System.currentTimeMillis() - start);
            double failureRate = breaker.calculateFailureRate();
            if (failureRate > breaker.getFailureThresholdPercentage()) {
                breaker.transitionTo(new OpenState(breaker));
            }
            return breaker.getFallback();
        }
    }
}
