package com.example.inherited.web;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/v1/authors")
public abstract class AbstractAuthorController {

    @GetMapping("/{id}")
    public abstract String getAuthor(String id);

    public String describe() {
        return "";
    }

    @DeleteMapping("/{id}")
    public String deleteAuthor(String id) {
        return id;
    }
}
