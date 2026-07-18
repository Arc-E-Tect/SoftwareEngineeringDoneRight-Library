package com.arc_e_tect.example.spring.typeleakage.persistence;

import jakarta.persistence.Entity;

@Entity
public class OrderEntity {

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
