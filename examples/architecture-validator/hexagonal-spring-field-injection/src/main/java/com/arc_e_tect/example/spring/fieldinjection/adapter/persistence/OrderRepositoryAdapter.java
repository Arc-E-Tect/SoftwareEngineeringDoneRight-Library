package com.arc_e_tect.example.spring.fieldinjection.adapter.persistence;

import com.arc_e_tect.example.spring.fieldinjection.application.domain.Order;
import com.arc_e_tect.example.spring.fieldinjection.application.port.outbound.OrderPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepositoryAdapter implements OrderPort {

    @Autowired
    private PersistenceGateway gateway;

    @Override
    public void save(Order order) {
        gateway.persist(order);
    }
}
