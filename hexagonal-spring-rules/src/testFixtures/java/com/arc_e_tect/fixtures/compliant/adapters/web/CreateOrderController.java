package com.arc_e_tect.fixtures.compliant.adapters.web;

import com.arc_e_tect.fixtures.compliant.application.port.in.CreateOrderUseCase;
import org.springframework.stereotype.Controller;

@Controller
public class CreateOrderController {

    private final CreateOrderUseCase createOrderUseCase;

    public CreateOrderController(CreateOrderUseCase createOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
    }

    public void create() {
        createOrderUseCase.create();
    }
}
