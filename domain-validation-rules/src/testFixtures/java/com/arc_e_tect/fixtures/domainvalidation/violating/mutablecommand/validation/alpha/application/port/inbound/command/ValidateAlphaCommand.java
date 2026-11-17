package com.arc_e_tect.fixtures.domainvalidation.violating.mutablecommand.validation.alpha.application.port.inbound.command;

public final class ValidateAlphaCommand {

    private String input;

    public ValidateAlphaCommand(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("input cannot be null or empty");
        }
        this.input = input;
    }

    public String input() {
        return input;
    }

    public void replaceInput(String input) {
        this.input = input;
    }
}
