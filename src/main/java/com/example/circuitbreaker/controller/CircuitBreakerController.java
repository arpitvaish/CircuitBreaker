package com.example.circuitbreaker.controller;

import com.example.circuitbreaker.core.CircuitBreaker;
import com.example.circuitbreaker.service.RemoteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CircuitBreakerController {
    private final CircuitBreaker circuitBreaker;
    private final RemoteService remoteService;

    public CircuitBreakerController(CircuitBreaker circuitBreaker, RemoteService remoteService) {
        this.circuitBreaker = circuitBreaker;
        this.remoteService = remoteService;
    }

    @GetMapping("/api/invoke")
    public String invoke() {
        return circuitBreaker.execute(remoteService::call);
    }
}
