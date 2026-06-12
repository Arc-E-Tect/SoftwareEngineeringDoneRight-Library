package com.arc_e_tect.example.spring.adapter.web;

import com.arc_e_tect.example.spring.application.service.OrderService;
import org.springframework.stereotype.Controller;

@Controller
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    public void submit(String id) {
        orderService.createOrder(id);
    }
}