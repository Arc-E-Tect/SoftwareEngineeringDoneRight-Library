package com.example.fixture;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConstantPathControllerFixture {

    private static final String BY_USERNAME_PATH = "/api/users/{username}";

    @GetMapping(BY_USERNAME_PATH)
    public String getUser() {
        return "";
    }

    @GetMapping(dynamicPath())
    public String getDynamic() {
        return "";
    }

    private static String dynamicPath() {
        return "/api/dynamic";
    }
}
