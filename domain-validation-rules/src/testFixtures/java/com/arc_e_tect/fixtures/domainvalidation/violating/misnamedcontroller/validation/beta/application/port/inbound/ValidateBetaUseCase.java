package com.arc_e_tect.fixtures.domainvalidation.violating.misnamedcontroller.validation.beta.application.port.inbound;

import com.arc_e_tect.fixtures.domainvalidation.violating.misnamedcontroller.validation.beta.application.port.inbound.command.ValidateBetaCommand;
import com.arc_e_tect.fixtures.domainvalidation.violating.misnamedcontroller.validation.beta.application.port.inbound.result.ValidateBetaResult;

public interface ValidateBetaUseCase {

    ValidateBetaResult validate(ValidateBetaCommand command);
}
