package com.arc_e_tect.example.spring.rulesdisabled.adapter.web;

import com.arc_e_tect.example.spring.rulesdisabled.application.port.inbound.OrderUseCase;
import org.springframework.stereotype.Controller;

@Controller
public class OrderController {

    private final OrderUseCase orderUseCase;

    public OrderController(OrderUseCase orderUseCase) {
        this.orderUseCase = orderUseCase;
    }

    public void submit(String id) {
        orderUseCase.createOrder(id);
    }
}
