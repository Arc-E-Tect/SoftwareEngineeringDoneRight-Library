package com.arc_e_tect.example.spring.typeleakage.application.port.outbound;

import com.arc_e_tect.example.spring.typeleakage.persistence.OrderEntity;

public interface OrderPort {

    OrderEntity findById(String id);
}
