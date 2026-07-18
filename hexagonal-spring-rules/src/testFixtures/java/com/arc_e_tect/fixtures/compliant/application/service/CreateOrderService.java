package com.arc_e_tect.fixtures.compliant.application.service;

import com.arc_e_tect.fixtures.compliant.application.port.in.CreateOrderUseCase;
import com.arc_e_tect.fixtures.compliant.application.port.out.OrderStorePort;

public class CreateOrderService implements CreateOrderUseCase {

    private final OrderStorePort orderStorePort;

    public CreateOrderService(OrderStorePort orderStorePort) {
        this.orderStorePort = orderStorePort;
    }

    @Override
    public void create() {
        orderStorePort.save();
    }
}
