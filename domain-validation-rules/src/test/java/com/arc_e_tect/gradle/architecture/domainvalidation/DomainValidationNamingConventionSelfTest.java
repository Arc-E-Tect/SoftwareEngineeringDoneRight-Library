package com.arc_e_tect.gradle.architecture.domainvalidation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("The naming convention rule")
class DomainValidationNamingConventionSelfTest extends RuleSelfTestSupport {

    @Test
    @DisplayName("lets a controller be named after the generated interface it implements")
    void controllerNamedAfterItsApiPasses() {
        configure("compliant");
        enableNamingConventions();

        assertDoesNotThrow(new DomainValidationNamingConventionTest()::controllersAreNamedAfterTheirApi);
    }

    @Test
    @DisplayName("fails when a controller is not named after the generated interface it implements")
    void misnamedControllerFails() {
        configure("violating.misnamedcontroller");
        enableNamingConventions();

        assertThrows(AssertionError.class, new DomainValidationNamingConventionTest()::controllersAreNamedAfterTheirApi);
    }

    @Test
    @DisplayName("is skipped unless naming conventions are enabled")
    void ruleIsSkippedWithoutNamingConventions() {
        configure("violating.misnamedcontroller");

        assertThrows(TestAbortedException.class, new DomainValidationNamingConventionTest()::controllersAreNamedAfterTheirApi);
    }
}
