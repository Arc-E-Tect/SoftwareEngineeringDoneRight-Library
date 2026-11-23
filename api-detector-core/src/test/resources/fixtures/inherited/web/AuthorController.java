package com.example.inherited.web;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthorController extends AbstractAuthorController {

    @Override
    public String getAuthor(String id) {
        return id;
    }

    @Override
    public String describe() {
        return "author";
    }
}
