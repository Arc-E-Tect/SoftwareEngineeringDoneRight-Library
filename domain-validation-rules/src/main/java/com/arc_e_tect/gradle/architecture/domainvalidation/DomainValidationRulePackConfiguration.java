package com.arc_e_tect.gradle.architecture.domainvalidation;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.PackageMatchers;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Internal configuration of the Domain Validation API rule pack.
 *
 * <p>The pack reads the properties the Architecture Validator plugin sets on its
 * {@code testArchitecture} task, and three of its own, which the consuming project sets on
 * that task with {@code systemProperty}:
 * <ul>
 *   <li>{@code architectureValidator.domainValidation.validationsPackages} - the packages that
 *       hold a bounded context's validations, one sub-package per validation; required</li>
 *   <li>{@code architectureValidator.domainValidation.sharedPackages} - the names of the
 *       sub-packages of a validations package that every validation shares, rather than
 *       validations themselves; {@code verdict} by default</li>
 *   <li>{@code architectureValidator.domainValidation.executionPackages} - the package
 *       patterns of the asynchronous execution component; {@code com.arc_e_tect.validation.async..}
 *       by default</li>
 * </ul>
 *
 * @since 0.1.0
 */
final class DomainValidationRulePackConfiguration {

    static final String VALIDATIONS_PACKAGES = "architectureValidator.domainValidation.validationsPackages";
    static final String SHARED_PACKAGES = "architectureValidator.domainValidation.sharedPackages";
    static final String EXECUTION_PACKAGES = "architectureValidator.domainValidation.executionPackages";

    private static final String BASE_PACKAGE = "architectureValidator.basePackage";
    private static final String IN_PORTS = "architectureValidator.inPorts";
    private static final String RULES_DISABLED = "architectureValidator.rules.disabled";
    private static final String NAMING_CONVENTIONS = "architectureValidator.namingConventions.enabled";

    private static final String DEFAULT_IN_PORTS = "..application.port.inbound..";
    private static final String DEFAULT_SHARED_PACKAGES = "verdict";
    private static final String DEFAULT_EXECUTION_PACKAGES = "com.arc_e_tect.validation.async..";

    private DomainValidationRulePackConfiguration() {
    }

    static String basePackage() {
        requireConfigured();
        return property(BASE_PACKAGE, "").trim();
    }

    static List<String> validationsPackages() {
        return list(VALIDATIONS_PACKAGES, "");
    }

    static Set<String> sharedPackages() {
        return Set.copyOf(list(SHARED_PACKAGES, DEFAULT_SHARED_PACKAGES));
    }

    static DescribedPredicate<String> executionPackages() {
        return PackageMatchers.of(list(EXECUTION_PACKAGES, DEFAULT_EXECUTION_PACKAGES));
    }

    static DescribedPredicate<String> inboundPorts() {
        return PackageMatchers.of(list(IN_PORTS, DEFAULT_IN_PORTS));
    }

    static boolean isRuleDisabled(String ruleId) {
        return list(RULES_DISABLED, "").contains(ruleId);
    }

    static boolean namingConventionsEnabled() {
        return Boolean.parseBoolean(property(NAMING_CONVENTIONS, "false"));
    }

    static void requireConfigured() {
        String basePackage = property(BASE_PACKAGE, "").trim();
        if (basePackage.isEmpty()) {
            throw new AssertionError(BASE_PACKAGE + " is not set. Configure it via the Architecture Validator plugin's architectureValidator { basePackage = 'com.example.payments' } extension.");
        }

        List<String> validationsPackages = validationsPackages();
        if (validationsPackages.isEmpty()) {
            throw new AssertionError(VALIDATIONS_PACKAGES + " is not set, so no validation would be checked. Set it on the testArchitecture task: tasks.named('testArchitecture') { systemProperty '" + VALIDATIONS_PACKAGES + "', 'com.example.payments.validation' }.");
        }

        List<String> outside = validationsPackages.stream()
                .filter(validationsPackage -> !validationsPackage.equals(basePackage)
                        && !validationsPackage.startsWith(basePackage + "."))
                .collect(Collectors.toList());
        if (!outside.isEmpty()) {
            throw new AssertionError(VALIDATIONS_PACKAGES + " names " + outside + ", outside the base package " + basePackage + ", where no class is imported. Name packages inside the base package.");
        }
    }

    private static String property(String key, String defaultValue) {
        return System.getProperty(key, defaultValue);
    }

    private static List<String> list(String key, String defaultValue) {
        String value = property(key, "").trim();
        String effective = value.isEmpty() ? defaultValue : value;
        return Arrays.stream(effective.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toList());
    }
}
