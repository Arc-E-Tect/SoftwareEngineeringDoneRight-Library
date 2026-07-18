package com.arc_e_tect.fixtures.regression.serviceImplementsPort.application.service;

import com.arc_e_tect.fixtures.regression.serviceImplementsPort.application.port.in.CreateOrderUseCase;
import com.arc_e_tect.fixtures.regression.serviceImplementsPort.application.port.out.OrderStorePort;
import org.springframework.stereotype.Service;

/**
 * A {@code @Service} that implements its own in-port, the standard Hexagonal wiring.
 * Regression fixture for {@code servicesShouldNotAccessRepositoriesDirectly}: implementing
 * an in-port is a real ArchUnit-visible dependency and must not be mistaken for reaching
 * into an adapter/repository package.
 */
@Service
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
