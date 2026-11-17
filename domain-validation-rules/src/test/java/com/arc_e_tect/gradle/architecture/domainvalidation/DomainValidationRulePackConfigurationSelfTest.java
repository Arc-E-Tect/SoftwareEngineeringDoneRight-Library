package com.arc_e_tect.gradle.architecture.domainvalidation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("The rule pack's configuration")
class DomainValidationRulePackConfigurationSelfTest extends RuleSelfTestSupport {

    @Test
    @DisplayName("is accepted when the base package and the validations packages are set")
    void acceptsACompleteConfiguration() {
        configure("compliant");

        assertDoesNotThrow(new DomainValidationRulePackConfigurationTest()::validationsPackagesAreConfigured);
    }

    @Test
    @DisplayName("is refused when no validations package is set, so no rule passes on an empty import")
    void refusesAMissingValidationsPackage() {
        configure("compliant");
        clearValidationsPackages();

        assertThrows(AssertionError.class, new DomainValidationRulePackConfigurationTest()::validationsPackagesAreConfigured);
    }

    @Test
    @DisplayName("is refused when a validations package lies outside the base package, where no class is imported")
    void refusesAValidationsPackageOutsideTheBasePackage() {
        configure("compliant");
        setValidationsPackages("com.example.elsewhere.validation");

        assertThrows(AssertionError.class, new DomainValidationRulePackConfigurationTest()::validationsPackagesAreConfigured);
    }
}
