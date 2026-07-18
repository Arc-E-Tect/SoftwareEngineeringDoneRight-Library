package com.arc_e_tect.example.spring.fieldinjection.application.port.outbound;

import com.arc_e_tect.example.spring.fieldinjection.application.domain.Order;

public interface OrderPort {

    void save(Order order);
}
