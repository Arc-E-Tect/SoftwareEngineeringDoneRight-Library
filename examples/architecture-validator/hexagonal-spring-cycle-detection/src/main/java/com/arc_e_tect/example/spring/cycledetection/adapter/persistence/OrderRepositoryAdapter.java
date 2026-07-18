package com.arc_e_tect.example.spring.cycledetection.adapter.persistence;

import com.arc_e_tect.example.spring.cycledetection.adapter.notification.OrderNotifier;
import com.arc_e_tect.example.spring.cycledetection.application.domain.Order;
import com.arc_e_tect.example.spring.cycledetection.application.port.outbound.OrderPort;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepositoryAdapter implements OrderPort {

    private final OrderNotifier notifier;

    public OrderRepositoryAdapter(OrderNotifier notifier) {
        this.notifier = notifier;
    }

    @Override
    public void save(Order order) {
        notifier.notifySaved(order.id());
    }

    public void recordNotification(String id) {
        // Example back-reference target used to complete the package cycle.
    }
}
