package com.arc_e_tect.fixtures.domainvalidation.violating.shareddependsonslice.validation.verdict;

import com.arc_e_tect.fixtures.domainvalidation.violating.shareddependsonslice.validation.alpha.application.port.inbound.result.ValidateAlphaResult;

/** A shared verdict that knows about one slice. */
public enum Outcome {
    VALID,
    INVALID;

    public static Outcome of(ValidateAlphaResult.Outcome outcome) {
        return outcome == ValidateAlphaResult.Outcome.VALID ? VALID : INVALID;
    }
}
