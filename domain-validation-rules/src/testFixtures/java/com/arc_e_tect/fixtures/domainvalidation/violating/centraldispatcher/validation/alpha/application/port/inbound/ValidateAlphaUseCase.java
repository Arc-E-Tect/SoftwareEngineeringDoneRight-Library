package com.arc_e_tect.fixtures.domainvalidation.violating.centraldispatcher.validation.alpha.application.port.inbound;

import com.arc_e_tect.fixtures.domainvalidation.violating.centraldispatcher.validation.alpha.application.port.inbound.command.ValidateAlphaCommand;
import com.arc_e_tect.fixtures.domainvalidation.violating.centraldispatcher.validation.alpha.application.port.inbound.result.ValidateAlphaResult;

public interface ValidateAlphaUseCase {

    ValidateAlphaResult validate(ValidateAlphaCommand command);
}
