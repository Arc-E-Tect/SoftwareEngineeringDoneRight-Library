package com.arc_e_tect.fixtures.violating.dependency.coreDependsOnAdapter.domain.model;

import com.arc_e_tect.fixtures.violating.dependency.coreDependsOnAdapter.adapters.persistence.AdapterThing;

public class CoreUsesAdapter {

    private final AdapterThing adapterThing;

    public CoreUsesAdapter(AdapterThing adapterThing) {
        this.adapterThing = adapterThing;
    }

    public void execute() {
        adapterThing.execute();
    }
}
