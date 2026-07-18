package com.arc_e_tect.example.spring.typeleakage.application.service;

import com.arc_e_tect.example.spring.typeleakage.application.port.inbound.OrderUseCase;
import com.arc_e_tect.example.spring.typeleakage.application.port.outbound.OrderPort;

public class OrderService implements OrderUseCase {

    private final OrderPort orderPort;

    public OrderService(OrderPort orderPort) {
        this.orderPort = orderPort;
    }

    @Override
    public void createOrder(String id) {
        orderPort.findById(id);
    }
}
