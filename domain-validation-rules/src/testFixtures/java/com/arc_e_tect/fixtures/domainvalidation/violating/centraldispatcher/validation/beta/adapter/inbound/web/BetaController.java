package com.arc_e_tect.fixtures.domainvalidation.violating.centraldispatcher.validation.beta.adapter.inbound.web;

import com.arc_e_tect.fixtures.domainvalidation.execution.ValidationExecution;
import com.arc_e_tect.fixtures.domainvalidation.violating.centraldispatcher.validation.beta.adapter.inbound.web.api.BetaApi;
import com.arc_e_tect.fixtures.domainvalidation.violating.centraldispatcher.validation.beta.adapter.inbound.web.api.BetaRequest;
import com.arc_e_tect.fixtures.domainvalidation.violating.centraldispatcher.validation.beta.adapter.inbound.web.api.BetaResponse;
import com.arc_e_tect.fixtures.domainvalidation.violating.centraldispatcher.validation.beta.application.port.inbound.ValidateBetaUseCase;
import com.arc_e_tect.fixtures.domainvalidation.violating.centraldispatcher.validation.beta.application.port.inbound.result.ValidateBetaResult;
import org.springframework.web.bind.annotation.RestController;

@RestController
class BetaController implements BetaApi {

    private final ValidateBetaUseCase useCase;
    private final BetaMapper mapper = new BetaMapper();
    private final ValidationExecution execution = new ValidationExecution();

    BetaController(ValidateBetaUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public BetaResponse validateBeta(BetaRequest request) {
        var command = mapper.toCommand(request);
        return execution.run("beta", () -> switch (useCase.validate(command)) {
            case ValidateBetaResult.Checked checked -> mapper.toResponse(checked);
            case ValidateBetaResult.Refused refused -> throw new IllegalStateException(refused.reason());
        });
    }
}
