package com.example.circuitbreaker.service;

import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class RemoteService {
    private final Random random = new Random();

    public String call() {
        if (random.nextInt(10) < 4) {
            throw new RuntimeException("Remote call failed");
        }
        return "Remote call succeeded";
    }
}
