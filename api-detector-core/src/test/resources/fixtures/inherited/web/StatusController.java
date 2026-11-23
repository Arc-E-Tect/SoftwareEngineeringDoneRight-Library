package com.example.inherited.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Fully qualified supertype, a supertype that is not among the scanned sources, its own
// class-level mapping instead of the inherited one, and its own method mapping instead of the
// inherited one.
@RestController
@RequestMapping("/status")
public class StatusController implements com.example.inherited.api.StatusApi, java.io.Serializable {

    @Override
    public String ping() {
        return "";
    }

    @Override
    @GetMapping("/details")
    public String details() {
        return "";
    }
}
