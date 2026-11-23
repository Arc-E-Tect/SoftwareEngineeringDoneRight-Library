package com.example.inherited.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// The supertype is a sibling nested type, referenced by its simple name: neither an import nor
// the package finds it, only the file it is declared in.
public final class SiblingControllers {

    private SiblingControllers() {}

    interface SiblingApi {

        @GetMapping("/v1/siblings")
        String siblings();
    }

    @RestController
    static class SiblingController implements SiblingApi {

        @Override
        public String siblings() {
            return "";
        }
    }
}
