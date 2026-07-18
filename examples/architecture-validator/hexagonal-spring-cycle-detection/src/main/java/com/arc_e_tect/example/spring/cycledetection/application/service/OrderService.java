package com.arc_e_tect.example.spring.cycledetection.application.service;

import com.arc_e_tect.example.spring.cycledetection.application.domain.Order;
import com.arc_e_tect.example.spring.cycledetection.application.port.inbound.OrderUseCase;
import com.arc_e_tect.example.spring.cycledetection.application.port.outbound.OrderPort;

public class OrderService implements OrderUseCase {

    private final OrderPort orderPort;

    public OrderService(OrderPort orderPort) {
        this.orderPort = orderPort;
    }

    @Override
    public void createOrder(String id) {
        orderPort.save(new Order(id));
    }
}
