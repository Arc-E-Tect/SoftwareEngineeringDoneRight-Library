package com.arc_e_tect.fixtures.domainvalidation.violating.misnamedcontroller.validation.alpha.application.port.inbound.result;

public sealed interface ValidateAlphaResult permits ValidateAlphaResult.Checked, ValidateAlphaResult.Refused {

    enum Outcome { VALID, INVALID }

    record Checked(Outcome outcome) implements ValidateAlphaResult {
    }

    record Refused(String reason) implements ValidateAlphaResult {
    }
}
