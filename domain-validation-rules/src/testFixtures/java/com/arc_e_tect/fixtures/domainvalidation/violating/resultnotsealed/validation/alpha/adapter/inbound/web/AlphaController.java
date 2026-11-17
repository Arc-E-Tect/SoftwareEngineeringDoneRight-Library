package com.arc_e_tect.fixtures.domainvalidation.violating.resultnotsealed.validation.alpha.adapter.inbound.web;

import com.arc_e_tect.fixtures.domainvalidation.execution.ValidationExecution;
import com.arc_e_tect.fixtures.domainvalidation.violating.resultnotsealed.validation.alpha.adapter.inbound.web.api.AlphaApi;
import com.arc_e_tect.fixtures.domainvalidation.violating.resultnotsealed.validation.alpha.adapter.inbound.web.api.AlphaRequest;
import com.arc_e_tect.fixtures.domainvalidation.violating.resultnotsealed.validation.alpha.adapter.inbound.web.api.AlphaResponse;
import com.arc_e_tect.fixtures.domainvalidation.violating.resultnotsealed.validation.alpha.application.port.inbound.ValidateAlphaUseCase;
import com.arc_e_tect.fixtures.domainvalidation.violating.resultnotsealed.validation.alpha.application.port.inbound.command.ValidateAlphaCommand;
import com.arc_e_tect.fixtures.domainvalidation.violating.resultnotsealed.validation.alpha.application.port.inbound.result.ValidateAlphaResult;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AlphaController implements AlphaApi {

    private final ValidateAlphaUseCase useCase;
    private final ValidationExecution execution = new ValidationExecution();

    AlphaController(ValidateAlphaUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public AlphaResponse validateAlpha(AlphaRequest request) {
        return execution.run("alpha", () -> switch (useCase.validate(new ValidateAlphaCommand(request.input()))) {
            case ValidateAlphaResult.Checked checked -> new AlphaResponse(checked.outcome().name());
            default -> throw new IllegalStateException("not a verdict");
        });
    }
}
