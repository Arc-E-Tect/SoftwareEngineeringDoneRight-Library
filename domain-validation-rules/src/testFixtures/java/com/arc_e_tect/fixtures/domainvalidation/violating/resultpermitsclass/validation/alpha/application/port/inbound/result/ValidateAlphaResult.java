package com.arc_e_tect.fixtures.domainvalidation.violating.resultpermitsclass.validation.alpha.application.port.inbound.result;

public sealed interface ValidateAlphaResult permits ValidateAlphaResult.Checked, ValidateAlphaResult.Refused {

    enum Outcome { VALID, INVALID }

    record Checked(Outcome outcome) implements ValidateAlphaResult {
    }

    final class Refused implements ValidateAlphaResult {

        private final String reason;

        public Refused(String reason) {
            this.reason = reason;
        }

        public String reason() {
            return reason;
        }
    }
}
