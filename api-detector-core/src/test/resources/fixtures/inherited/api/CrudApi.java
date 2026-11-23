package com.example.inherited.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/v1/shelves")
public interface CrudApi<T> {

    @GetMapping("/{id}")
    T read(String id);
}
