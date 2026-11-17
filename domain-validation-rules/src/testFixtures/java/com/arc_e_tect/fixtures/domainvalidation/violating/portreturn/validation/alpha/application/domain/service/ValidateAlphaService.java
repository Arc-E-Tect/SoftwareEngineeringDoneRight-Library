package com.arc_e_tect.fixtures.domainvalidation.violating.portreturn.validation.alpha.application.domain.service;

import com.arc_e_tect.fixtures.domainvalidation.violating.portreturn.validation.alpha.application.port.inbound.ValidateAlphaUseCase;
import com.arc_e_tect.fixtures.domainvalidation.violating.portreturn.validation.alpha.application.port.inbound.command.ValidateAlphaCommand;
import com.arc_e_tect.fixtures.domainvalidation.violating.portreturn.validation.alpha.application.port.inbound.result.ValidateAlphaResult;

public class ValidateAlphaService implements ValidateAlphaUseCase {

    @Override
    public ValidateAlphaResult validate(ValidateAlphaCommand command) {
        return new ValidateAlphaResult.Refused(command.input());
    }

    @Override
    public boolean isValid(ValidateAlphaCommand command) {
        return !command.input().isBlank();
    }
}
