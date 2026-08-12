package com.example.fixture;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PlainControllerFixture {

    @GetMapping("/plain")
    public String plain() {
        return "view-name";
    }
}
