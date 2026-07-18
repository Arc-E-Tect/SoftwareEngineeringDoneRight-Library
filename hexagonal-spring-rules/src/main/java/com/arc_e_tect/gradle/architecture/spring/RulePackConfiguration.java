package com.arc_e_tect.gradle.architecture.spring;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Internal configuration for Spring Hexagonal architecture rule pack tests.
 * 
 * <p>This class provides centralized access to system properties that define architectural
 * package boundaries for Spring applications following the Hexagonal (ports and adapters) pattern.
 * Property names are prefixed with {@code architectureValidator} for consistency with the
 * Architecture Validator plugin configuration.
 * 
 * <p>Supported properties:
 * <ul>
 *   <li>{@code architectureValidator.basePackage} - Root package to validate</li>
 *   <li>{@code architectureValidator.inPorts} - Comma-separated in-port packages</li>
 *   <li>{@code architectureValidator.outPorts} - Comma-separated out-port packages</li>
 *   <li>{@code architectureValidator.domainModel} - Comma-separated domain model packages</li>
 *   <li>{@code architectureValidator.adapters} - Comma-separated adapter packages (legacy aggregate)</li>
 *   <li>{@code architectureValidator.inboundAdapters} - Comma-separated inbound adapter packages, merged with {@code adapters}</li>
 *   <li>{@code architectureValidator.outboundAdapters} - Comma-separated outbound adapter packages, merged with {@code adapters}</li>
 *   <li>{@code architectureValidator.applicationServices} - Comma-separated application service packages</li>
 *   <li>{@code architectureValidator.rules.disabled} - Comma-separated rule identifiers to skip</li>
 *   <li>{@code architectureValidator.namingConventions.enabled} - Enables optional naming convention rules</li>
 * </ul>
 *
 * @since 0.4.0
 */
final class RulePackConfiguration {

    private RulePackConfiguration() {
    }

    static String basePackage() {
        requireConfigured();
        return property("architectureValidator.basePackage", "");
    }

    static String[] inPorts() {
        return packages("architectureValidator.inPorts");
    }

    static String[] outPorts() {
        return packages("architectureValidator.outPorts");
    }

    static String[] domainModel() {
        return packages("architectureValidator.domainModel");
    }

    static String[] adapters() {
        // The Architecture Validator plugin's inboundAdapters/outboundAdapters is the preferred
        // split package layout; architectureValidator.adapters remains a legacy aggregate
        // fallback. Merge all three so this rule pack matches whichever layout a consumer
        // actually configured instead of only recognizing the legacy aggregate property.
        return Stream.of(
                        packages("architectureValidator.adapters"),
                        packages("architectureValidator.inboundAdapters"),
                        packages("architectureValidator.outboundAdapters"))
                .flatMap(Arrays::stream)
                .distinct()
                .toArray(String[]::new);
    }

    static String[] applicationServices() {
        return packages("architectureValidator.applicationServices");
    }

    static boolean isRuleDisabled(String ruleId) {
        return disabledRules().contains(ruleId);
    }

    static boolean namingConventionsEnabled() {
        return Boolean.parseBoolean(property("architectureValidator.namingConventions.enabled", "false"));
    }

    static void requireConfigured() {
        String configuredBasePackage = property("architectureValidator.basePackage", "").trim();

        if (configuredBasePackage.isEmpty()) {
            throw new AssertionError("architectureValidator.basePackage is not set. Configure it via the Architecture Validator plugin's architectureValidator { basePackage = 'com.example.myapp' } extension.");
        }

        if (inPorts().length == 0 && outPorts().length == 0 && domainModel().length == 0) {
            throw new AssertionError("architectureValidator.inPorts, architectureValidator.outPorts, and architectureValidator.domainModel are all empty. Configure at least one of these via the Architecture Validator plugin's architectureValidator { inPorts = 'com.example.myapp.application.port.inbound'; outPorts = 'com.example.myapp.application.port.outbound'; domainModel = 'com.example.myapp.domain.model' } extension.");
        }
    }

    static String[] merge(String[] dynamicPackages, String... fixedPackages) {
        return Stream.concat(Arrays.stream(fixedPackages), Arrays.stream(dynamicPackages))
                .toArray(String[]::new);
    }

    private static String property(String key, String defaultValue) {
        return System.getProperty(key, defaultValue);
    }

    private static String[] packages(String key) {
        List<String> values = Arrays.stream(property(key, "").split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toList());
        return values.toArray(new String[0]);
    }

    private static Set<String> disabledRules() {
        return Arrays.stream(property("architectureValidator.rules.disabled", "").split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());
    }
}