package com.arc_e_tect.fixtures.domainvalidation.violating.portparameter.validation.beta.application.port.inbound.command;

public final class ValidateBetaCommand {

    private final String input;

    public ValidateBetaCommand(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("input cannot be null or empty");
        }
        this.input = input;
    }

    public String input() {
        return input;
    }
}
