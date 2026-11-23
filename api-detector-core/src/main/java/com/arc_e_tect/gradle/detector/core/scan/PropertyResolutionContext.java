package com.arc_e_tect.gradle.detector.core.scan;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Out-of-band knowledge that lets {@link LiteralPathResolver} resolve a value that isn't
 * literally present in the file it's parsing: a merged key/value property map (typically loaded
 * by the caller from one or more {@code .properties}/{@code .yml}/{@code .yaml} files) and the set
 * of static helper methods - identified only by declaring type's simple name and method name, e.g.
 * {@code "ApiEndpoints.get"} - whose single string-literal argument should be treated as a key into
 * that map.
 *
 * <p>This class deliberately carries no file-loading or YAML-parsing logic of its own; building
 * the merged property map (including flattening nested YAML into dotted keys) is the caller's
 * responsibility, since only the caller knows which files are relevant to a given scan and how
 * they should be merged/precedence-ordered.</p>
 *
 * <p>Immutable and safe to share/reuse across an entire scan run.</p>
 */
public final class PropertyResolutionContext {

    private static final PropertyResolutionContext EMPTY = new PropertyResolutionContext(Map.of(), Set.of());

    private final Map<String, String> properties;
    private final Set<String> helperMethods;

    private PropertyResolutionContext(Map<String, String> properties, Set<String> helperMethods) {
        this.properties = Map.copyOf(properties);
        this.helperMethods = Set.copyOf(helperMethods);
    }

    /**
     * Returns the context without any out-of-band knowledge.
     *
     * @return a context with no properties and no helper-method conventions - resolution
     *         behaviour is then identical to {@link LiteralPathResolver#resolve(Expression)}.
     */
    public static PropertyResolutionContext empty() {
        return EMPTY;
    }

    /**
     * Creates a context from a merged property map and a set of helper-method conventions.
     *
     * @param properties    the merged property key/value map, e.g. loaded from one or more
     *                      {@code .properties}/{@code .yml} files
     * @param helperMethods helper-method conventions as {@code "SimpleClassName.methodName"}
     *                      strings, e.g. {@code "ApiEndpoints.get"}
     * @return a context holding copies of {@code properties} and {@code helperMethods}; a
     *         {@code null} argument counts as empty
     */
    public static PropertyResolutionContext of(Map<String, String> properties, Set<String> helperMethods) {
        return new PropertyResolutionContext(
                properties == null ? Map.of() : new HashMap<>(properties),
                helperMethods == null ? Set.of() : new HashSet<>(helperMethods));
    }

    /**
     * @return a context with this context's helper methods and its properties added to
     *         {@code fallbacks}, so a key present in both keeps this context's value
     */
    PropertyResolutionContext withFallbacks(Map<String, String> fallbacks) {
        Map<String, String> merged = new HashMap<>(fallbacks);
        merged.putAll(properties);
        return new PropertyResolutionContext(merged, helperMethods);
    }

    /**
     * Looks up a property.
     *
     * @param key the property key, e.g. {@code "users.by-username"}
     * @return the value for {@code key}, if present in the merged property map.
     */
    public Optional<String> lookup(String key) {
        return Optional.ofNullable(properties.get(key));
    }

    /**
     * Tells whether a static method is a configured helper-method convention.
     *
     * @param simpleClassName the simple name of the method's declaring type, e.g. {@code "ApiEndpoints"}
     * @param methodName      the method's name, e.g. {@code "get"}
     * @return true if {@code "simpleClassName.methodName"} was configured as a helper-method
     *         convention.
     */
    public boolean isHelperMethod(String simpleClassName, String methodName) {
        return helperMethods.contains(simpleClassName + "." + methodName);
    }

    /**
     * Returns the properties of this context.
     *
     * @return an unmodifiable view of the merged property map.
     */
    public Map<String, String> properties() {
        return properties;
    }
}
