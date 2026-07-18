package com.arc_e_tect.example.spring.namingconventions.application.service;

import com.arc_e_tect.example.spring.namingconventions.application.domain.Order;
import com.arc_e_tect.example.spring.namingconventions.application.port.inbound.OrderCommands;
import com.arc_e_tect.example.spring.namingconventions.application.port.outbound.OrderStore;

public class OrderService implements OrderCommands {

    private final OrderStore orderStore;

    public OrderService(OrderStore orderStore) {
        this.orderStore = orderStore;
    }

    @Override
    public void createOrder(String id) {
        orderStore.save(new Order(id));
    }
}
