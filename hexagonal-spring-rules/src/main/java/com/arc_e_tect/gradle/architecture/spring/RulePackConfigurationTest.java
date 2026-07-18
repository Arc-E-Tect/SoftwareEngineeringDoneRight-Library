package com.arc_e_tect.gradle.architecture.spring;

import org.junit.jupiter.api.Test;

/**
 * Fail-fast configuration validation for the Spring rule pack.
 * 
 * <p>This rule ensures the consuming project configures the minimum
 * Architecture Validator properties so architecture checks do not pass
 * vacuously with empty class imports.
 *
 * @see RulePackConfiguration
 * @since 1.0.0
 */
class RulePackConfigurationTest {

    /**
     * Validates that required architecture properties are configured.
     */
    @Test
    void requiredArchitecturePropertiesShouldBeConfigured() {
        RulePackConfiguration.requireConfigured();
    }
}
