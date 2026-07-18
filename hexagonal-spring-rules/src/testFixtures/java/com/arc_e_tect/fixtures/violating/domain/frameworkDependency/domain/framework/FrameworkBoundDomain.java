package com.arc_e_tect.fixtures.violating.domain.frameworkDependency.domain.framework;

import org.springframework.util.Assert;

public class FrameworkBoundDomain {

    private final String value;

    public FrameworkBoundDomain(String value) {
        Assert.hasText(value, "value must not be empty");
        this.value = value;
    }

    public String value() {
        return value;
    }
}
