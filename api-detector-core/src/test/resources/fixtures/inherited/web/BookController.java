package com.example.inherited.web;

import com.example.inherited.api.BooksApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookController implements BooksApi {

    @Override
    public ResponseEntity<String> getBook(String id) {
        return ResponseEntity.ok(id);
    }

    @Override
    public ResponseEntity<String> createBook(String book) {
        return ResponseEntity.ok(book);
    }

    public String notAnEndpoint() {
        return "";
    }
}
