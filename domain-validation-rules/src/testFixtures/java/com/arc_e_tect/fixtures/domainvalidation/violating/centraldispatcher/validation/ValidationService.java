package com.arc_e_tect.fixtures.domainvalidation.violating.centraldispatcher.validation;

import com.arc_e_tect.fixtures.domainvalidation.violating.centraldispatcher.validation.alpha.application.port.inbound.ValidateAlphaUseCase;
import com.arc_e_tect.fixtures.domainvalidation.violating.centraldispatcher.validation.alpha.application.port.inbound.command.ValidateAlphaCommand;
import com.arc_e_tect.fixtures.domainvalidation.violating.centraldispatcher.validation.beta.application.port.inbound.ValidateBetaUseCase;
import com.arc_e_tect.fixtures.domainvalidation.violating.centraldispatcher.validation.beta.application.port.inbound.command.ValidateBetaCommand;

/** A central service that dispatches to every validation: a rejected alternative. */
public class ValidationService {

    private final ValidateAlphaUseCase alpha;
    private final ValidateBetaUseCase beta;

    public ValidationService(ValidateAlphaUseCase alpha, ValidateBetaUseCase beta) {
        this.alpha = alpha;
        this.beta = beta;
    }

    public Object validate(String validation, String input) {
        return "alpha".equals(validation)
                ? alpha.validate(new ValidateAlphaCommand(input))
                : beta.validate(new ValidateBetaCommand(input));
    }
}
