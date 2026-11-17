package com.arc_e_tect.gradle.architecture.domainvalidation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("The inbound port rules")
class DomainValidationInboundPortSelfTest extends RuleSelfTestSupport {

    @Test
    @DisplayName("let a use case take a command and return a result")
    void useCaseWithCommandAndResultPasses() {
        configure("compliant");

        assertDoesNotThrow(new DomainValidationInboundPortTest()::useCasesTakeACommandAndReturnAResult);
    }

    @Test
    @DisplayName("fail when a use case takes something other than a command")
    void useCaseTakingAStringFails() {
        configure("violating.portparameter");

        assertThrows(AssertionError.class, new DomainValidationInboundPortTest()::useCasesTakeACommandAndReturnAResult);
    }

    @Test
    @DisplayName("fail when a use case returns something other than a result")
    void useCaseReturningABooleanFails() {
        configure("violating.portreturn");

        assertThrows(AssertionError.class, new DomainValidationInboundPortTest()::useCasesTakeACommandAndReturnAResult);
    }

    @Test
    @DisplayName("let a result be a sealed interface of records")
    void sealedResultOfRecordsPasses() {
        configure("compliant");

        assertDoesNotThrow(new DomainValidationInboundPortTest()::useCaseResultsAreSealed);
    }

    @Test
    @DisplayName("fail when a result is not sealed")
    void unsealedResultFails() {
        configure("violating.resultnotsealed");

        assertThrows(AssertionError.class, new DomainValidationInboundPortTest()::useCaseResultsAreSealed);
    }

    @Test
    @DisplayName("fail when a sealed result permits a class that is not a record")
    void sealedResultPermittingAClassFails() {
        configure("violating.resultpermitsclass");

        assertThrows(AssertionError.class, new DomainValidationInboundPortTest()::useCaseResultsAreSealed);
    }

    @Test
    @DisplayName("let a command have final fields only")
    void immutableCommandPasses() {
        configure("compliant");

        assertDoesNotThrow(new DomainValidationInboundPortTest()::commandsAreImmutable);
    }

    @Test
    @DisplayName("fail when a command has a field that is not final")
    void mutableCommandFails() {
        configure("violating.mutablecommand");

        assertThrows(AssertionError.class, new DomainValidationInboundPortTest()::commandsAreImmutable);
    }
}
