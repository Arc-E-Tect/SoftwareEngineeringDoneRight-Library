package com.arc_e_tect.fixtures.domainvalidation.violating.misnamedcontroller.validation.alpha.adapter.inbound.web.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Stands in for the interface generated from the contract. */
public interface AlphaApi {

    @PostMapping("/v1/validations/alpha")
    AlphaResponse validateAlpha(@RequestBody AlphaRequest request);
}
