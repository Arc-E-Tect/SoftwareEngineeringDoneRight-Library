package com.arc_e_tect.fixtures.violating.domain.frameworkDependency.domain.model;

public class GoodDomain {

    private final String value;

    public GoodDomain(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
