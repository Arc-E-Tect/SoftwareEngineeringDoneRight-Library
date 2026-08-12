package com.example.fixture;

public class NestedControllerFixture {

    @org.springframework.web.bind.annotation.RestController
    @org.springframework.web.bind.annotation.RequestMapping("/nested")
    public static class Inner {

        @org.springframework.web.bind.annotation.GetMapping("/ping")
        public String ping() {
            return "pong";
        }
    }
}
