package com.arc_e_tect.fixtures.domainvalidation.violating.generatedtypesleak.validation.beta.application.domain.service;

import com.arc_e_tect.fixtures.domainvalidation.violating.generatedtypesleak.validation.verdict.Outcome;
import com.arc_e_tect.fixtures.domainvalidation.violating.generatedtypesleak.validation.beta.application.port.inbound.ValidateBetaUseCase;
import com.arc_e_tect.fixtures.domainvalidation.violating.generatedtypesleak.validation.beta.application.port.inbound.command.ValidateBetaCommand;
import com.arc_e_tect.fixtures.domainvalidation.violating.generatedtypesleak.validation.beta.application.port.inbound.result.ValidateBetaResult;

public class ValidateBetaService implements ValidateBetaUseCase {

    @Override
    public ValidateBetaResult validate(ValidateBetaCommand command) {
        Outcome outcome = command.input().isBlank() ? Outcome.INVALID : Outcome.VALID;
        return new ValidateBetaResult.Checked(outcome == Outcome.VALID
                ? ValidateBetaResult.Outcome.VALID
                : ValidateBetaResult.Outcome.INVALID);
    }
}
