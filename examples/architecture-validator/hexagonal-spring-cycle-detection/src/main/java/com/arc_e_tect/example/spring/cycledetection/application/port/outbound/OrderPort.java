package com.arc_e_tect.example.spring.cycledetection.application.port.outbound;

import com.arc_e_tect.example.spring.cycledetection.application.domain.Order;

public interface OrderPort {

    void save(Order order);
}
