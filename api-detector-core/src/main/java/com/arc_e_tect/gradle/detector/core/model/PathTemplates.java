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

    /**
     * Removes a leading {@code basePath} segment from {@code path}, both normalised via
     * {@link #normalize(String)} first. Meant for a path recorded against the full request URL a
     * real client actually sends - e.g. one read from a WireMock stub mapping file, which always
     * includes whatever deployment-time context path the server runs under - so it can be compared
     * against a path template that never includes one, such as an OpenAPI-declared path or one
     * read from a {@code @RequestMapping} annotation.
     *
     * @param path     the path to strip {@code basePath} from
     * @param basePath the base path to remove; blank or {@code "/"} - i.e. no real base path -
     *                 leaves {@code path} unchanged
     * @return {@code path}, normalised, with a leading {@code basePath} removed; unchanged
     *         (other than normalising) when {@code path} doesn't actually start with
     *         {@code basePath}, so a mismatched configuration never corrupts an unrelated path
     */
    public static String stripBasePath(String path, String basePath) {
        String normalizedPath = normalize(path);
        String normalizedBase = normalize(basePath);
        if (normalizedBase.equals("/")) {
            return normalizedPath;
        }
        if (normalizedPath.equals(normalizedBase)) {
            return "/";
        }
        if (normalizedPath.startsWith(normalizedBase + "/")) {
            return normalizedPath.substring(normalizedBase.length());
        }
        return normalizedPath;
    }

    private static String blankToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
