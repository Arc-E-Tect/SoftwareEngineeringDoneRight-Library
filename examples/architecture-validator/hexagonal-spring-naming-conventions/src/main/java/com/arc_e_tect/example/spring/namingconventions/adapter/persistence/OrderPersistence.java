package com.arc_e_tect.example.spring.namingconventions.adapter.persistence;

import com.arc_e_tect.example.spring.namingconventions.application.domain.Order;
import com.arc_e_tect.example.spring.namingconventions.application.port.outbound.OrderStore;
import org.springframework.stereotype.Repository;

@Repository
public class OrderPersistence implements OrderStore {

    @Override
    public void save(Order order) {
        // Example persistence operation.
    }
}
