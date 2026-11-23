package com.example.inherited.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

// The shape OpenAPI Generator's Spring "interfaceOnly" output takes: a base-path placeholder with
// an empty default, and the paths as constants referenced through the interface's own name.
@RequestMapping("${openapi.books.base-path:}")
public interface BooksApi {

    String PATH_GET_BOOK = "/v1/books/{id}";
    String PATH_CREATE_BOOK = "/v1/books";

    @RequestMapping(method = RequestMethod.GET, value = BooksApi.PATH_GET_BOOK, produces = { "application/json" })
    ResponseEntity<String> getBook(@PathVariable("id") String id);

    @RequestMapping(method = RequestMethod.POST, value = BooksApi.PATH_CREATE_BOOK)
    ResponseEntity<String> createBook(@RequestBody String book);

    @GetMapping("/v1/books")
    default ResponseEntity<String> listBooks() {
        return ResponseEntity.ok("");
    }
}
