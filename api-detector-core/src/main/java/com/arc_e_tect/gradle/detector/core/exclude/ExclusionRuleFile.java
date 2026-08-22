package com.arc_e_tect.gradle.detector.core.exclude;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads a YAML exclusion rule file - used both for a project's own external exclusion file(s) and
 * for {@link WellKnownExclusionSets}' bundled classpath resources, which share the same format:
 *
 * <pre>
 * exclusions:
 *   - "/actuator/health"
 *   - "GET /actuator/**"
 * </pre>
 */
public final class ExclusionRuleFile {

    private ExclusionRuleFile() {}

    /**
     * Loads exclusion rules from {@code file}.
     *
     * @param file the YAML file to load
     * @return the parsed rules, in file order
     * @throws IOException              if {@code file} cannot be read
     * @throws IllegalArgumentException if a rule string in the file is malformed, or the file's
     *                                   top-level shape is not the expected {@code exclusions: [...]}
     */
    public static List<ExclusionRule> load(File file) throws IOException {
        try (InputStream in = Files.newInputStream(file.toPath())) {
            return load(in, file.toString());
        }
    }

    /**
     * Loads exclusion rules from a classpath resource, bundled with a plugin jar.
     *
     * @param resourceLoader a class from whose classloader {@code resourcePath} is resolved
     * @param resourcePath   the classpath resource path, e.g.
     *                       {@code "wellknown/spring-boot-actuator.yaml"}
     * @return the parsed rules, in file order
     * @throws IOException              if the resource is missing or cannot be read
     * @throws IllegalArgumentException if a rule string in the resource is malformed, or the
     *                                   resource's top-level shape is not the expected form
     */
    public static List<ExclusionRule> loadResource(Class<?> resourceLoader, String resourcePath) throws IOException {
        try (InputStream in = resourceLoader.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Missing bundled resource: " + resourcePath);
            }
            return load(in, resourcePath);
        }
    }

    private static List<ExclusionRule> load(InputStream in, String sourceDescription) {
        Object loaded = new Yaml().load(in);
        if (loaded == null) {
            return List.of();
        }
        if (!(loaded instanceof Map<?, ?> map) || !(map.get("exclusions") instanceof List<?> entries)) {
            throw new IllegalArgumentException(
                    "Invalid exclusion file " + sourceDescription + ": expected a top-level 'exclusions:' list.");
        }
        List<ExclusionRule> rules = new ArrayList<>(entries.size());
        for (Object entry : entries) {
            if (!(entry instanceof String s)) {
                throw new IllegalArgumentException("Invalid exclusion file " + sourceDescription
                        + ": every entry under 'exclusions' must be a string.");
            }
            rules.add(ExclusionRule.parse(s));
        }
        return rules;
    }
}
