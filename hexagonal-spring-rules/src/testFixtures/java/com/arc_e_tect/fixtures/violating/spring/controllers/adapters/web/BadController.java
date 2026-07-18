package com.arc_e_tect.fixtures.violating.spring.controllers.adapters.web;

import com.arc_e_tect.fixtures.violating.spring.controllers.application.service.ConcreteService;
import org.springframework.stereotype.Controller;

@Controller
public class BadController {

    private final ConcreteService concreteService;

    public BadController(ConcreteService concreteService) {
        this.concreteService = concreteService;
    }

    public void execute() {
        concreteService.execute();
    }
}
