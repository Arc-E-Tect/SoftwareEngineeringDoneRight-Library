package com.arc_e_tect.fixtures.domainvalidation.violating.resultnotsealed.validation.alpha.application.port.inbound.result;

public interface ValidateAlphaResult {

    enum Outcome { VALID, INVALID }

    record Checked(Outcome outcome) implements ValidateAlphaResult {
    }

    record Refused(String reason) implements ValidateAlphaResult {
    }
}
