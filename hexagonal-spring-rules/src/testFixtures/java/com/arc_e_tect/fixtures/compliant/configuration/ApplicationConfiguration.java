package com.arc_e_tect.fixtures.compliant.configuration;

import com.arc_e_tect.fixtures.compliant.application.port.outbound.OrderStorePort;
import com.arc_e_tect.fixtures.compliant.application.service.CreateOrderService;

public class ApplicationConfiguration {

    public CreateOrderService createOrderService(OrderStorePort orderStorePort) {
        return new CreateOrderService(orderStorePort);
    }
}
