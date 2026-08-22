package com.arc_e_tect.gradle.detector.core.exclude;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

/**
 * Registry of bundled, named exclusion rule sets for well-known framework endpoint families -
 * e.g. {@value #SPRING_BOOT_ACTUATOR}, covering Spring Boot Actuator's management endpoints,
 * which are provided by the framework's own auto-configuration rather than a hand-written
 * {@code @RestController} and so are structurally invisible to {@code ControllerScanner} even
 * when they are documented and fully functional at runtime.
 *
 * <p>Each set is a classpath resource under {@code wellknown/} using the same YAML format
 * {@link ExclusionRuleFile} already reads for a project's own external exclusion files.</p>
 */
public final class WellKnownExclusionSets {

    /** Name of the bundled Spring Boot Actuator exclusion set. */
    public static final String SPRING_BOOT_ACTUATOR = "spring-boot-actuator";

    private static final Map<String, String> RESOURCES_BY_NAME =
            Map.of(SPRING_BOOT_ACTUATOR, "wellknown/spring-boot-actuator.yaml");

    private WellKnownExclusionSets() {}

    /**
     * Resolves {@code name} to its bundled exclusion rules.
     *
     * @param name the well-known set's name, e.g. {@value #SPRING_BOOT_ACTUATOR}
     * @return the rules bundled for that name
     * @throws IllegalArgumentException if {@code name} is not a recognised well-known set
     */
    public static List<ExclusionRule> resolve(String name) {
        String resourcePath = RESOURCES_BY_NAME.get(name);
        if (resourcePath == null) {
            throw new IllegalArgumentException("Unknown well-known exclusion set '" + name
                    + "'. Recognised sets: " + String.join(", ", RESOURCES_BY_NAME.keySet()) + ".");
        }
        try {
            return ExclusionRuleFile.loadResource(WellKnownExclusionSets.class, resourcePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load bundled exclusion set '" + name + "'.", e);
        }
    }
}
