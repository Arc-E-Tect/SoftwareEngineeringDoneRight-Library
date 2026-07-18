package com.arc_e_tect.fixtures.violating.domain.domainDependency.domain.model;

import com.arc_e_tect.fixtures.violating.domain.domainDependency.application.port.out.ForbiddenPort;

public class BadDomain {

    private final ForbiddenPort forbiddenPort;

    public BadDomain(ForbiddenPort forbiddenPort) {
        this.forbiddenPort = forbiddenPort;
    }

    public void execute() {
        forbiddenPort.send();
    }
}
