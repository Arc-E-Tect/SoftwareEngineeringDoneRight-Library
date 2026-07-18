package com.arc_e_tect.fixtures.violating.spring.repositories.application.service;

import com.arc_e_tect.fixtures.violating.spring.repositories.adapters.persistence.BadRepository;

public class RepositoryConsumer {

    private final BadRepository badRepository;

    public RepositoryConsumer(BadRepository badRepository) {
        this.badRepository = badRepository;
    }

    public void execute() {
        badRepository.save();
    }
}
