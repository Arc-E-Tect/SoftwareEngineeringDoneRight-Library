package com.example.fixture;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class SimpleControllerFixture {

    @GetMapping
    public String listUsers() {
        return "";
    }

    @GetMapping("/{id}")
    public String getUser(@PathVariable("id") Long id) {
        return "";
    }

    @PostMapping("/{id:[0-9]+}")
    public String createUser() {
        return "";
    }

    @PutMapping(path = "/{id}")
    public String replaceUser() {
        return "";
    }

    @PatchMapping("/{id}")
    public String patchUser() {
        return "";
    }

    @DeleteMapping("/{id}")
    public String deleteUser() {
        return "";
    }

    @RequestMapping(value = "/{id}/archive", method = RequestMethod.POST)
    public String archiveUser() {
        return "";
    }

    @RequestMapping(value = {"/{id}/tags", "/{id}/labels"}, method = {RequestMethod.GET, RequestMethod.HEAD})
    public String userTags() {
        return "";
    }

    @RequestMapping("/{id}/summary")
    public String userSummary() {
        return "";
    }

    public void notAnEndpoint() {
    }
}
