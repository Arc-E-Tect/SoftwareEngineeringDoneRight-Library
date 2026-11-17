package com.arc_e_tect.fixtures.domainvalidation.violating.misnamedcontroller.validation.alpha.adapter.inbound.web;

import com.arc_e_tect.fixtures.domainvalidation.execution.ValidationExecution;
import com.arc_e_tect.fixtures.domainvalidation.violating.misnamedcontroller.validation.alpha.adapter.inbound.web.api.AlphaApi;
import com.arc_e_tect.fixtures.domainvalidation.violating.misnamedcontroller.validation.alpha.adapter.inbound.web.api.AlphaRequest;
import com.arc_e_tect.fixtures.domainvalidation.violating.misnamedcontroller.validation.alpha.adapter.inbound.web.api.AlphaResponse;
import com.arc_e_tect.fixtures.domainvalidation.violating.misnamedcontroller.validation.alpha.application.port.inbound.ValidateAlphaUseCase;
import com.arc_e_tect.fixtures.domainvalidation.violating.misnamedcontroller.validation.alpha.application.port.inbound.result.ValidateAlphaResult;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AlphaHandler implements AlphaApi {

    private final ValidateAlphaUseCase useCase;
    private final AlphaMapper mapper = new AlphaMapper();
    private final ValidationExecution execution = new ValidationExecution();

    AlphaHandler(ValidateAlphaUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public AlphaResponse validateAlpha(AlphaRequest request) {
        var command = mapper.toCommand(request);
        return execution.run("alpha", () -> switch (useCase.validate(command)) {
            case ValidateAlphaResult.Checked checked -> mapper.toResponse(checked);
            case ValidateAlphaResult.Refused refused -> throw new IllegalStateException(refused.reason());
        });
    }
}
