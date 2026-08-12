package com.arc_e_tect.gradle.detector.core.model;

/**
 * Utility methods for normalising and combining Spring MVC / OpenAPI path templates so that
 * they can be compared regardless of superficial formatting differences.
 */
public final class PathTemplates {

    private PathTemplates() {}

    /**
     * Joins a base path and a sub-path into a single normalised path template.
     *
     * @param base the base path, e.g. a class-level {@code @RequestMapping} value; may be blank
     * @param sub  the sub-path, e.g. a method-level mapping value; may be blank
     * @return the combined, normalised path, always starting with {@code "/"}
     */
    public static String join(String base, String sub) {
        return normalize((blankToEmpty(base) + "/" + blankToEmpty(sub)));
    }

    /**
     * Normalises a path template: ensures a leading {@code "/"}, collapses repeated
     * {@code "/"} separators, strips path-variable regex constraints
     * (e.g. {@code "{id:[0-9]+}"} becomes {@code "{id}"}), and removes a trailing
     * {@code "/"} (other than for the root path {@code "/"} itself).
     *
     * @param path the raw path template
     * @return the normalised path template
     */
    public static String normalize(String path) {
        String result = blankToEmpty(path);
        if (!result.startsWith("/")) {
            result = "/" + result;
        }
        result = result.replaceAll("/{2,}", "/");
        result = result.replaceAll("\\{([^:}]+):[^}]*}", "{$1}");
        if (result.length() > 1 && result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /**
     * Returns whether {@code segment} is a Spring/OpenAPI path-variable placeholder, i.e.
     * wrapped in curly braces, e.g. {@code "{id}"}.
     *
     * @param segment a single, {@code "/"}-delimited path segment
     * @return {@code true} when {@code segment} is a placeholder segment
     */
    public static boolean isPlaceholder(String segment) {
        return segment.startsWith("{") && segment.endsWith("}");
    }

    private static String blankToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
