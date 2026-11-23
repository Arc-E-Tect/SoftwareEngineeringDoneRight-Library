package com.example.inherited.web;

import com.example.inherited.api.Paths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${reviews.base-path}")
public class ReviewController {

    @GetMapping(Paths.REVIEWS)
    public String listReviews() {
        return "";
    }

    @GetMapping("${reviews.item-path:/items}/{id}")
    public String getReview(String id) {
        return id;
    }
}
