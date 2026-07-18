package com.arc_e_tect.example.spring.typeleakage.adapter.persistence;

import com.arc_e_tect.example.spring.typeleakage.application.port.outbound.OrderPort;
import com.arc_e_tect.example.spring.typeleakage.persistence.OrderEntity;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepositoryAdapter implements OrderPort {

    @Override
    public OrderEntity findById(String id) {
        OrderEntity entity = new OrderEntity();
        entity.setId(id);
        return entity;
    }
}
