package com.arc_e_tect.fixtures.violating.spring.services.adapters.service;

import com.arc_e_tect.fixtures.violating.spring.services.adapters.persistence.BadRepository;
import org.springframework.stereotype.Service;

@Service
public class BadService {

    private final BadRepository badRepository;

    public BadService(BadRepository badRepository) {
        this.badRepository = badRepository;
    }

    public void execute() {
        badRepository.save();
    }
}
