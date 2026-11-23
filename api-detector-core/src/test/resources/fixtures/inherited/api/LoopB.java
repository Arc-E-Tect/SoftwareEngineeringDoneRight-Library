package com.example.inherited.api;

import org.springframework.web.bind.annotation.GetMapping;

public interface LoopB extends LoopA {

    @GetMapping("/v1/loops")
    String loop();
}
