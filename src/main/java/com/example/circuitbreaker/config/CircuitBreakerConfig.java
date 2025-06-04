package com.example.circuitbreaker.config;

import com.example.circuitbreaker.core.CircuitBreaker;
import com.example.circuitbreaker.core.CountBasedEvaluationStrategy;
import com.example.circuitbreaker.core.EvaluationStrategy;
import com.example.circuitbreaker.core.TimeBasedEvaluationStrategy;
import com.example.circuitbreaker.model.CircuitBreakerType;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CircuitBreakerConfig {
    @Bean
    public CircuitBreaker circuitBreaker(MeterRegistry registry) {
       /* return CircuitBreaker.builder()
            .failureThresholdPercentage(50)
            .requestVolumeThreshold(4)
            .openStateTimeoutMillis(1000)
            .fallback(new Thread("Fallback response"))
            .onStateChange((oldState, newState) -> System.out.printf("State changed from %s to %s%n", oldState, newState))
            .meterRegistry(registry)
            .build();*/

        CircuitBreaker breaker = CircuitBreaker.builder()
                .failureThresholdPercentage(50)
                .type(CircuitBreakerType.TIME_BASED)
               // .type(CircuitBreakerType.COUNT_BASED)
                .requestVolumeThreshold(4)
                .slidingWindowDurationSeconds(10)
                .openStateTimeoutMillis(1000)
                .fallback(() -> "fallback")
                .onStateChange((oldState, newState) ->
                        System.out.printf("State changed from %s to %s%n", oldState, newState))
                .meterRegistry(registry)
                .build();

        // Choose your strategy here
        //EvaluationStrategy strategy = new CountBasedEvaluationStrategy(50, 4);
        // EvaluationStrategy strategy = new TimeBasedEvaluationStrategy(50, 10);

       // breaker.setEvaluationStrategy(strategy);
        return breaker;

    }
}
