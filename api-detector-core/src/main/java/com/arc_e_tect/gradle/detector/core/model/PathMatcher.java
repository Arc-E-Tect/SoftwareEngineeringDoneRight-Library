package com.arc_e_tect.gradle.detector.core.model;

/**
 * Compares two normalised path templates for structural equivalence, tolerant of path-variable
 * naming differences (e.g. Spring's {@code "{id}"} against an OpenAPI {@code "{userId}"}).
 */
public final class PathMatcher {

    private PathMatcher() {}

    /**
     * Returns whether two normalised path templates are structurally equivalent: same number of
     * {@code "/"}-delimited segments, with every literal segment equal and every placeholder
     * segment (e.g. {@code "{id}"}) aligned with a placeholder segment on the other side,
     * regardless of the placeholder's variable name.
     *
     * @param pathA a path template normalised via {@link PathTemplates#normalize(String)}
     * @param pathB a path template normalised via {@link PathTemplates#normalize(String)}
     * @return {@code true} when the two templates describe the same set of concrete paths
     */
    public static boolean matches(String pathA, String pathB) {
        String[] segmentsA = segments(pathA);
        String[] segmentsB = segments(pathB);
        if (segmentsA.length != segmentsB.length) {
            return false;
        }
        for (int i = 0; i < segmentsA.length; i++) {
            String a = segmentsA[i];
            String b = segmentsB[i];
            boolean aPlaceholder = PathTemplates.isPlaceholder(a);
            boolean bPlaceholder = PathTemplates.isPlaceholder(b);
            if (aPlaceholder != bPlaceholder) {
                return false;
            }
            if (!aPlaceholder && !a.equals(b)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether {@code concretePath} - a resolved, literal path with no placeholder
     * segments of its own, such as a Spring Cloud Contract example URL (e.g. {@code "/items/1"})
     * - is a valid instance of {@code templatePath} (e.g. {@code "/items/{id}"}): same number of
     * segments, with every placeholder segment in {@code templatePath} accepting any segment
     * value in {@code concretePath} at that position, and every literal segment in
     * {@code templatePath} required to equal the corresponding segment in {@code concretePath}
     * exactly.
     *
     * <p>Unlike {@link #matches(String, String)}, this comparison is asymmetric: it exists for
     * comparing a genuinely concrete, resolved example path against a template, not two
     * templates against each other. When {@code concretePath} is itself a template (e.g. it also
     * contains {@code "{id}"}-style placeholders), this method still behaves correctly and
     * produces the same result {@link #matches(String, String)} would, since a placeholder
     * segment on the template side accepts any segment value, including one that happens to look
     * like a placeholder itself.</p>
     *
     * <p><strong>Known imprecision:</strong> because a concrete path carries no information about
     * which template it was written to exemplify, a concrete path can structurally satisfy a
     * template it was never actually meant to verify, if the two happen to share the same
     * segment count and literal segments outside the templated position (e.g. a contract example
     * for {@code "/items/count"} would also satisfy a template {@code "/items/{id}"}, since
     * {@code "count"} is accepted as a value for {@code "{id}"}). This is the same class of
     * heuristic imprecision already accepted elsewhere in the detector family (e.g.
     * {@link HttpVerb#ANY} matching any verb); it is not something this method can resolve on
     * its own, since doing so would require knowing the concrete path's original intent, not
     * just its shape.</p>
     *
     * @param concretePath a resolved, literal path with no placeholder segments of its own
     * @param templatePath a path template normalised via {@link PathTemplates#normalize(String)}
     * @return {@code true} when {@code concretePath} is a valid instance of {@code templatePath}
     */
    public static boolean matchesConcrete(String concretePath, String templatePath) {
        String[] concreteSegments = segments(concretePath);
        String[] templateSegments = segments(templatePath);
        if (concreteSegments.length != templateSegments.length) {
            return false;
        }
        for (int i = 0; i < templateSegments.length; i++) {
            String template = templateSegments[i];
            if (PathTemplates.isPlaceholder(template)) {
                continue;
            }
            if (!template.equals(concreteSegments[i])) {
                return false;
            }
        }
        return true;
    }

    private static String[] segments(String path) {
        if ("/".equals(path)) {
            return new String[0];
        }
        String trimmed = path.startsWith("/") ? path.substring(1) : path;
        return trimmed.split("/");
    }
}
