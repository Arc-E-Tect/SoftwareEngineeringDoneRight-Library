package com.arc_e_tect.fixtures.compliant.domain.model;

import java.util.UUID;

public class Order {

    private final UUID id;

    public Order(UUID id) {
        this.id = id;
    }

    public UUID id() {
        return id;
    }
}
