package com.example.fixture;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NoBasePathControllerFixture {

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
