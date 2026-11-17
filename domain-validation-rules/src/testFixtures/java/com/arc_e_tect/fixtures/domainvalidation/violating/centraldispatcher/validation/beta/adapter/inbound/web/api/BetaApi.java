package com.arc_e_tect.fixtures.domainvalidation.violating.centraldispatcher.validation.beta.adapter.inbound.web.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Stands in for the interface generated from the contract. */
public interface BetaApi {

    @PostMapping("/v1/validations/beta")
    BetaResponse validateBeta(@RequestBody BetaRequest request);
}
