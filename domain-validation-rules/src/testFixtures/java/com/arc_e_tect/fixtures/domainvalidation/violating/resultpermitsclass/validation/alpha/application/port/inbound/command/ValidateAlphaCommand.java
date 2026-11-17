package com.arc_e_tect.fixtures.domainvalidation.violating.resultpermitsclass.validation.alpha.application.port.inbound.command;

public final class ValidateAlphaCommand {

    private final String input;

    public ValidateAlphaCommand(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("input cannot be null or empty");
        }
        this.input = input;
    }

    public String input() {
        return input;
    }
}
