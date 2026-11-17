package com.arc_e_tect.fixtures.domainvalidation.violating.slicedependency.validation.beta.adapter.inbound.web;

import com.arc_e_tect.fixtures.domainvalidation.violating.slicedependency.validation.beta.adapter.inbound.web.api.BetaRequest;
import com.arc_e_tect.fixtures.domainvalidation.violating.slicedependency.validation.beta.adapter.inbound.web.api.BetaResponse;
import com.arc_e_tect.fixtures.domainvalidation.violating.slicedependency.validation.beta.application.port.inbound.command.ValidateBetaCommand;
import com.arc_e_tect.fixtures.domainvalidation.violating.slicedependency.validation.beta.application.port.inbound.result.ValidateBetaResult;

class BetaMapper {

    ValidateBetaCommand toCommand(BetaRequest request) {
        return new ValidateBetaCommand(request.input());
    }

    BetaResponse toResponse(ValidateBetaResult.Checked checked) {
        return new BetaResponse(checked.outcome().name());
    }
}
