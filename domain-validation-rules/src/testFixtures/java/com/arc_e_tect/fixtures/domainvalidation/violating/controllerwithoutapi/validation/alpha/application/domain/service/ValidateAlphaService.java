package com.arc_e_tect.fixtures.domainvalidation.violating.controllerwithoutapi.validation.alpha.application.domain.service;

import com.arc_e_tect.fixtures.domainvalidation.violating.controllerwithoutapi.validation.verdict.Outcome;
import com.arc_e_tect.fixtures.domainvalidation.violating.controllerwithoutapi.validation.alpha.application.port.inbound.ValidateAlphaUseCase;
import com.arc_e_tect.fixtures.domainvalidation.violating.controllerwithoutapi.validation.alpha.application.port.inbound.command.ValidateAlphaCommand;
import com.arc_e_tect.fixtures.domainvalidation.violating.controllerwithoutapi.validation.alpha.application.port.inbound.result.ValidateAlphaResult;

public class ValidateAlphaService implements ValidateAlphaUseCase {

    @Override
    public ValidateAlphaResult validate(ValidateAlphaCommand command) {
        Outcome outcome = command.input().isBlank() ? Outcome.INVALID : Outcome.VALID;
        return new ValidateAlphaResult.Checked(outcome == Outcome.VALID
                ? ValidateAlphaResult.Outcome.VALID
                : ValidateAlphaResult.Outcome.INVALID);
    }
}
