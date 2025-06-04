com.example.circuitbreaker
│
├── controller
│    └── CircuitBreakerController.java
│
├── service
│    └── CircuitBreakerService.java
│    └── RemoteService.java
│
├── circuitbreaker
│    ├── CircuitBreaker.java
│    ├── CircuitBreakerState.java
│    └── CircuitBreakerType.java
│
├── model
│    └── ExecutionResult.java
│
├── metrics
│    └── CircuitBreakerMetrics.java
│
├── exception
│    └── CircuitBreakerOpenException.java
│
└── CircuitBreakerApp.java
# Circuit Breaker Java (Spring Boot)

## Overview
This project implements a custom Circuit Breaker pattern with:
- Sliding window failure detection (count-based and time-based)
- State management with transitions between CLOSED, OPEN, and HALF_OPEN states
- Fallback handling and metrics integration

## Package Structure
- **controller**: Spring MVC REST API controllers (MVC pattern)
- **service**: Business logic encapsulation (Service pattern)
- **circuitbreaker**: Core circuit breaker logic using State and Strategy patterns
- **model**: DTOs representing data structures
- **exception**: Custom exceptions for clean error handling

## Design Patterns Used

### 1. **State Pattern**
Circuit breaker states (CLOSED, OPEN, HALF_OPEN) are implemented as separate classes conforming to the `CircuitBreakerState` interface. The `CircuitBreaker` delegates behavior to the current state object, allowing dynamic behavior changes based on state.

### 2. **Strategy Pattern**
Supports different sliding window failure calculation types: COUNT_BASED and TIME_BASED. This is selected using the `CircuitBreakerType` enum and implemented internally with different request log cleanup strategies.

### 3. **Builder Pattern**
The `CircuitBreaker` uses a Builder class for flexible and readable instantiation with various configuration parameters.

### 4. **Service Pattern**
Business logic is encapsulated in the `CircuitBreakerService` class, separating concerns and improving testability.

### 5. **MVC Pattern**
Spring Boot’s Model-View-Controller is applied in the REST controller layer to expose APIs.

---

## How to Run

- Build with Maven/Gradle
- Run Spring Boot Application
- Use `/api/invoke` to trigger remote calls with circuit breaker protection
- Use `/api/metrics` to view current circuit breaker metrics

---

## Future Improvements

- Add async support
- Integrate external monitoring dashboards
- Add circuit breaker persistence state storage
