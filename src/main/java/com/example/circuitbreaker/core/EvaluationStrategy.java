package com.example.circuitbreaker.core;

// com.example.circuitbreaker.core.EvaluationStrategy.java
public interface EvaluationStrategy {
    void recordSuccess();
    void recordFailure();
    boolean shouldTripOpen();
    void reset();
}
