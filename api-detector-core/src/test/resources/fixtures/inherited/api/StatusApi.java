package com.example.inherited.api;

import org.springframework.web.bind.annotation.GetMapping;

public interface StatusApi extends PingApi {

    @GetMapping("/summary")
    String details();
}
