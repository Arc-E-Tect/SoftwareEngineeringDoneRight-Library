package com.arc_e_tect.example.spring.application.service;

import com.arc_e_tect.example.spring.adapter.persistence.OrderRepository;
import com.arc_e_tect.example.spring.application.domain.model.Order;
import com.arc_e_tect.example.spring.application.port.inbound.OrderUseCase;

public class OrderService implements OrderUseCase {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public void createOrder(String id) {
        repository.save(new Order(id));
    }
}