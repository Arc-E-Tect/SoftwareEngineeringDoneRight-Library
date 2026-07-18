package com.arc_e_tect.example.spring.rulesdisabled.application.service.adapter;

import com.arc_e_tect.example.spring.rulesdisabled.adapter.persistence.OrderRepository;
import com.arc_e_tect.example.spring.rulesdisabled.application.domain.Order;
import com.arc_e_tect.example.spring.rulesdisabled.application.port.inbound.OrderUseCase;
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
