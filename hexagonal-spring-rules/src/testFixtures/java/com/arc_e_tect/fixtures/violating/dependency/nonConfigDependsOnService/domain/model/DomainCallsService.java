package com.arc_e_tect.fixtures.violating.dependency.nonConfigDependsOnService.domain.model;

import com.arc_e_tect.fixtures.violating.dependency.nonConfigDependsOnService.application.service.AppService;

public class DomainCallsService {

    private final AppService appService;

    public DomainCallsService(AppService appService) {
        this.appService = appService;
    }

    public void execute() {
        appService.execute();
    }
}
