package com.arc_e_tect.fixtures.violating.dependency.adapterDependsOnService.application.domain.service.adapter;

import com.arc_e_tect.fixtures.violating.dependency.adapterDependsOnService.application.domain.service.impl.AppServiceImpl;

public class AdapterCallsService {

    private final AppServiceImpl appService;

    public AdapterCallsService(AppServiceImpl appService) {
        this.appService = appService;
    }

    public void execute() {
        appService.execute();
    }
}
