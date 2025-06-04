package com.example.circuitbreaker.core.state;


import com.example.circuitbreaker.core.CircuitBreaker;

import java.util.function.Supplier;

/**
 * OPEN state: Requests are blocked until timeout.
 */
public class OpenState implements State {
    private final CircuitBreaker breaker;

    public OpenState(CircuitBreaker breaker) {
        this.breaker = breaker;
    }

    @Override
    public String handle(Supplier<String> remoteCall) {
        if (breaker.isOpenStateTimedOut()) {
            breaker.transitionTo(new HalfOpenState(breaker));
            return breaker.execute(remoteCall);  // Retry in HalfOpen
        } else {
            breaker.incrementBlockedRequest();
            return breaker.getFallback();
        }
    }
}
