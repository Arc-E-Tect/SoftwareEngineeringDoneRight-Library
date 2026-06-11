package com.example.spring.adapter.web;

import com.example.spring.application.service.OrderService;
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