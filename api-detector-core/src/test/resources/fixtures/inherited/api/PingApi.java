package com.example.inherited.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/ignored")
public interface PingApi {

    @GetMapping("/ping")
    String ping();
}
