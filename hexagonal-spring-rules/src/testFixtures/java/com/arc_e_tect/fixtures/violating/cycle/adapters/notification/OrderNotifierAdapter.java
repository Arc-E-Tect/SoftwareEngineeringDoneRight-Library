package com.arc_e_tect.fixtures.violating.cycle.adapters.notification;

import com.arc_e_tect.fixtures.violating.cycle.adapters.persistence.OrderRepositoryAdapter;

public class OrderNotifierAdapter {

    private final OrderRepositoryAdapter repository;

    public OrderNotifierAdapter(OrderRepositoryAdapter repository) {
        this.repository = repository;
    }
}
