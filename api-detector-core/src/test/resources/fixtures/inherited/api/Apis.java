package com.example.inherited.api;

import org.springframework.web.bind.annotation.GetMapping;

public final class Apis {

    private Apis() {}

    public interface Health {

        @GetMapping("/health")
        String health();
    }
}
