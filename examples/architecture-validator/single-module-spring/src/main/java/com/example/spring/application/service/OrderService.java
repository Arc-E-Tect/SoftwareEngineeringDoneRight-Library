package com.example.spring.application.service;

import com.example.spring.adapter.persistence.OrderRepository;
import com.example.spring.application.domain.model.Order;
import com.example.spring.application.port.in.OrderUseCase;
import org.springframework.stereotype.Service;

@Service
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