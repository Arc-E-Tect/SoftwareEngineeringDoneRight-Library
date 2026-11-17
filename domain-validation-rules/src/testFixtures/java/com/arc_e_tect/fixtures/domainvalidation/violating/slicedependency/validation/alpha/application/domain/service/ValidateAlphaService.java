package com.arc_e_tect.fixtures.domainvalidation.violating.slicedependency.validation.alpha.application.domain.service;

import com.arc_e_tect.fixtures.domainvalidation.violating.slicedependency.validation.beta.application.port.inbound.command.ValidateBetaCommand;
import com.arc_e_tect.fixtures.domainvalidation.violating.slicedependency.validation.alpha.application.port.inbound.ValidateAlphaUseCase;
import com.arc_e_tect.fixtures.domainvalidation.violating.slicedependency.validation.alpha.application.port.inbound.command.ValidateAlphaCommand;
import com.arc_e_tect.fixtures.domainvalidation.violating.slicedependency.validation.alpha.application.port.inbound.result.ValidateAlphaResult;

public class ValidateAlphaService implements ValidateAlphaUseCase {

    @Override
    public ValidateAlphaResult validate(ValidateAlphaCommand command) {
        var borrowed = new ValidateBetaCommand(command.input());
        return new ValidateAlphaResult.Refused(borrowed.input());
    }
}
