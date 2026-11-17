package com.arc_e_tect.fixtures.domainvalidation.violating.portparameter.validation.alpha.application.domain.service;

import com.arc_e_tect.fixtures.domainvalidation.violating.portparameter.validation.alpha.application.port.inbound.ValidateAlphaUseCase;
import com.arc_e_tect.fixtures.domainvalidation.violating.portparameter.validation.alpha.application.port.inbound.result.ValidateAlphaResult;

public class ValidateAlphaService implements ValidateAlphaUseCase {

    @Override
    public ValidateAlphaResult validate(String input) {
        return new ValidateAlphaResult.Refused(input);
    }
}
