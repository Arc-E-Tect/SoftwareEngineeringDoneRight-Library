package com.arc_e_tect.example.spring.adapter.web;

import com.arc_e_tect.example.spring.application.port.inbound.OrderUseCase;
import org.springframework.stereotype.Controller;

@Controller
public class OrderController {

    private final OrderUseCase orderService;

    public OrderController(OrderUseCase orderService) {
        this.orderService = orderService;
    }

    public void submit(String id) {
        orderService.createOrder(id);
    }
}