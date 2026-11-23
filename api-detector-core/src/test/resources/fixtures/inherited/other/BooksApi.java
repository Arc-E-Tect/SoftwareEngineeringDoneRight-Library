package com.example.inherited.other;

import org.springframework.web.bind.annotation.GetMapping;

// Same simple name as com.example.inherited.api.BooksApi: a controller importing that one must
// never inherit this one's mappings.
public interface BooksApi {

    @GetMapping("/wrong")
    String getBook(String id);
}
