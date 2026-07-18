package com.arc_e_tect.example.spring.namingconventions.adapter.web;

import com.arc_e_tect.example.spring.namingconventions.application.port.inbound.OrderCommands;
import org.springframework.stereotype.Controller;

@Controller
public class OrderController {

    private final OrderCommands orderCommands;

    public OrderController(OrderCommands orderCommands) {
        this.orderCommands = orderCommands;
    }

    public void submit(String id) {
        orderCommands.createOrder(id);
    }
}
