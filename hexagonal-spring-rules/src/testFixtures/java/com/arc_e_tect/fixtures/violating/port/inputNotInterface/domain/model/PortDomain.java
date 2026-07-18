package com.arc_e_tect.fixtures.violating.port.inputNotInterface.domain.model;

public class PortDomain {

    private final String value;

    public PortDomain(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
