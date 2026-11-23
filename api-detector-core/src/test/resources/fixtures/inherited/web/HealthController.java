package com.example.inherited.web;

import com.example.inherited.api.Apis;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController implements Apis.Health {

    @Override
    public String health() {
        return "";
    }
}
