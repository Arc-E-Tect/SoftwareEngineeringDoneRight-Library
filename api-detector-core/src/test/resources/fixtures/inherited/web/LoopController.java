package com.example.inherited.web;

import com.example.inherited.api.LoopA;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoopController implements LoopA {

    @Override
    public String loop() {
        return "";
    }
}
