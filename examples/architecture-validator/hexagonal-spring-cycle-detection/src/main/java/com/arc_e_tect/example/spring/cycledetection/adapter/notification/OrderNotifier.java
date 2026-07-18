package com.arc_e_tect.example.spring.cycledetection.adapter.notification;

import com.arc_e_tect.example.spring.cycledetection.adapter.persistence.OrderRepositoryAdapter;
import org.springframework.stereotype.Component;

@Component
public class OrderNotifier {

    private final OrderRepositoryAdapter repositoryAdapter;

    public OrderNotifier(OrderRepositoryAdapter repositoryAdapter) {
        this.repositoryAdapter = repositoryAdapter;
    }

    public void notifySaved(String id) {
        repositoryAdapter.recordNotification(id);
    }
}
