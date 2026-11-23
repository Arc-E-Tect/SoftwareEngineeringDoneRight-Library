package com.example.inherited.web;

import com.example.inherited.api.*;
import static com.example.inherited.api.Paths.REVIEWS;
import static java.util.Objects.*;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShelfController implements CrudApi<String> {

    @Override
    public String read(String id) {
        return id;
    }
}
