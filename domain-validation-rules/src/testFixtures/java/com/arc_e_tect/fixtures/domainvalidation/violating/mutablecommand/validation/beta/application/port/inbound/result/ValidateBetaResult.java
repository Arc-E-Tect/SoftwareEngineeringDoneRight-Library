package com.arc_e_tect.fixtures.domainvalidation.violating.mutablecommand.validation.beta.application.port.inbound.result;

public sealed interface ValidateBetaResult permits ValidateBetaResult.Checked, ValidateBetaResult.Refused {

    enum Outcome { VALID, INVALID }

    record Checked(Outcome outcome) implements ValidateBetaResult {
    }

    record Refused(String reason) implements ValidateBetaResult {
    }
}
