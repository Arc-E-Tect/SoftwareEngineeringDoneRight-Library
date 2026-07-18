package com.arc_e_tect.example.spring.namingconventions.application.port.outbound;

import com.arc_e_tect.example.spring.namingconventions.application.domain.Order;

public interface OrderStore {

    void save(Order order);
}
