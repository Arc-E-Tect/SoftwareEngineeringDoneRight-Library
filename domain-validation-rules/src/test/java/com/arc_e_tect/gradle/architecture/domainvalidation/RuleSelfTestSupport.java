package com.arc_e_tect.gradle.architecture.domainvalidation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs a rule class against a fixture bounded context, with the system properties the
 * Architecture Validator plugin would set, and restores them afterwards.
 */
abstract class RuleSelfTestSupport {

    static final String FIXTURES = "com.arc_e_tect.fixtures.domainvalidation";

    private static final String BASE_PACKAGE = "architectureValidator.basePackage";
    private static final String IN_PORTS = "architectureValidator.inPorts";
    private static final String RULES_DISABLED = "architectureValidator.rules.disabled";
    private static final String NAMING_CONVENTIONS = "architectureValidator.namingConventions.enabled";
    private static final String VALIDATIONS_PACKAGES = "architectureValidator.domainValidation.validationsPackages";
    private static final String SHARED_PACKAGES = "architectureValidator.domainValidation.sharedPackages";
    private static final String EXECUTION_PACKAGES = "architectureValidator.domainValidation.executionPackages";

    private static final List<String> KEYS = List.of(
            BASE_PACKAGE, IN_PORTS, RULES_DISABLED, NAMING_CONVENTIONS,
            VALIDATIONS_PACKAGES, SHARED_PACKAGES, EXECUTION_PACKAGES);

    private final Map<String, String> original = new HashMap<>();

    @BeforeEach
    void captureProperties() {
        KEYS.forEach(key -> original.put(key, System.getProperty(key)));
        KEYS.forEach(System::clearProperty);
    }

    @AfterEach
    void restoreProperties() {
        KEYS.forEach(key -> {
            String value = original.get(key);
            if (value == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, value);
            }
        });
    }

    /**
     * Configures the fixture bounded context {@code FIXTURES.<context>}, whose validations
     * live in its {@code validation} package, the way a consumer configures the plugin.
     */
    static void configure(String context) {
        String base = FIXTURES + "." + context;
        System.setProperty(BASE_PACKAGE, base);
        System.setProperty(IN_PORTS, "..application.port.inbound..");
        System.setProperty(VALIDATIONS_PACKAGES, base + ".validation");
        System.setProperty(EXECUTION_PACKAGES, FIXTURES + ".execution..");
    }

    static void setValidationsPackages(String value) {
        System.setProperty(VALIDATIONS_PACKAGES, value);
    }

    static void clearValidationsPackages() {
        System.clearProperty(VALIDATIONS_PACKAGES);
    }

    static void disableRule(String ruleId) {
        System.setProperty(RULES_DISABLED, ruleId);
    }

    static void enableNamingConventions() {
        System.setProperty(NAMING_CONVENTIONS, "true");
    }
}
