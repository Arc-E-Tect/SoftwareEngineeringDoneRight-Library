package com.arc_e_tect.fixtures.domainvalidation.violating.portparameter.validation.alpha.application.port.inbound;

import com.arc_e_tect.fixtures.domainvalidation.violating.portparameter.validation.alpha.application.port.inbound.result.ValidateAlphaResult;

public interface ValidateAlphaUseCase {

    ValidateAlphaResult validate(String input);
}
