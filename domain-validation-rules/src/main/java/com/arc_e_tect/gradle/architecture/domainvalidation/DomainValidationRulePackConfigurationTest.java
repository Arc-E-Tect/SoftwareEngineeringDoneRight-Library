package com.arc_e_tect.gradle.architecture.domainvalidation;

import org.junit.jupiter.api.Test;

/**
 * Fails fast when the rule pack is not configured, so that it cannot pass on an empty import.
 *
 * @see DomainValidationRulePackConfiguration
 * @since 0.1.0
 */
class DomainValidationRulePackConfigurationTest {

    /**
     * Validates that the base package and the validations packages are set, and that every
     * validations package lies inside the base package, where classes are imported.
     */
    @Test
    void validationsPackagesAreConfigured() {
        DomainValidationRulePackConfiguration.requireConfigured();
    }
}
