package com.example.circuitbreaker.core.state;

import java.util.function.Supplier;

public interface State {
    String handle(Supplier<String> remoteCall);
}
