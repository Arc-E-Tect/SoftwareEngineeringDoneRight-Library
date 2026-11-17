package com.arc_e_tect.fixtures.domainvalidation.violating.centraldispatcher.validation.alpha.adapter.inbound.web;

import com.arc_e_tect.fixtures.domainvalidation.violating.centraldispatcher.validation.alpha.adapter.inbound.web.api.AlphaRequest;
import com.arc_e_tect.fixtures.domainvalidation.violating.centraldispatcher.validation.alpha.adapter.inbound.web.api.AlphaResponse;
import com.arc_e_tect.fixtures.domainvalidation.violating.centraldispatcher.validation.alpha.application.port.inbound.command.ValidateAlphaCommand;
import com.arc_e_tect.fixtures.domainvalidation.violating.centraldispatcher.validation.alpha.application.port.inbound.result.ValidateAlphaResult;

class AlphaMapper {

    ValidateAlphaCommand toCommand(AlphaRequest request) {
        return new ValidateAlphaCommand(request.input());
    }

    AlphaResponse toResponse(ValidateAlphaResult.Checked checked) {
        return new AlphaResponse(checked.outcome().name());
    }
}
