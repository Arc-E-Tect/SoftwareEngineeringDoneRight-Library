package com.arc_e_tect.fixtures.violating.cycle.adapters.persistence;

import com.arc_e_tect.fixtures.violating.cycle.adapters.notification.OrderNotifierAdapter;

public class OrderRepositoryAdapter {

    private final OrderNotifierAdapter notifier;

    public OrderRepositoryAdapter(OrderNotifierAdapter notifier) {
        this.notifier = notifier;
    }
}
