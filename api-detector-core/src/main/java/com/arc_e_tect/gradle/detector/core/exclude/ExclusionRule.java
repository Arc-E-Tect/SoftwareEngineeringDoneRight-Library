package com.arc_e_tect.gradle.detector.core.exclude;

import com.arc_e_tect.gradle.detector.core.model.HttpVerb;

import java.util.Locale;

/**
 * One exclusion rule: an optional HTTP verb plus a path pattern that may use Ant-style
 * {@code *}/{@code **} wildcards, parsed from a single rule string used identically in a
 * plugin's DSL, external exclusion files, and bundled well-known sets.
 *
 * <p>Rule string grammar: {@code "[<VERB> ]<path-pattern>"} - e.g. {@code "/actuator/health"}
 * (any verb) or {@code "GET /actuator/**"} (verb-restricted). The verb, when present, is matched
 * case-insensitively and must be one of {@link HttpVerb}'s constants; a {@code *} path segment
 * matches exactly one segment, and a trailing {@code **} segment matches any remaining number of
 * segments - see {@link ExclusionMatcher}.</p>
 *
 * @param verb        the verb this rule restricts matching to, or {@link HttpVerb#ANY} when the
 *                     rule string carries no verb prefix
 * @param pathPattern the path pattern, e.g. {@code "/actuator/**"}; always starts with {@code "/"}
 */
public record ExclusionRule(HttpVerb verb, String pathPattern) {

    /**
     * Parses one rule string.
     *
     * @param rule the rule string, e.g. {@code "/actuator/health"} or {@code "GET /actuator/**"}
     * @return the parsed rule
     * @throws IllegalArgumentException if {@code rule} is blank, its path pattern does not start
     *                                   with {@code "/"}, or it names an unrecognised HTTP verb
     */
    public static ExclusionRule parse(String rule) {
        if (rule == null || rule.isBlank()) {
            throw new IllegalArgumentException("Exclusion rule must not be blank.");
        }
        String trimmed = rule.trim();
        int firstSpace = trimmed.indexOf(' ');
        if (firstSpace < 0) {
            return new ExclusionRule(HttpVerb.ANY, requirePath(trimmed, rule));
        }
        String maybeVerb = trimmed.substring(0, firstSpace);
        String rest = trimmed.substring(firstSpace + 1).trim();
        if (rest.startsWith("/")) {
            return new ExclusionRule(parseVerb(maybeVerb, rule), requirePath(rest, rule));
        }
        // No recognisable "VERB path" split (e.g. incidental extra whitespace before a single
        // path token) - treat the whole trimmed string as the path pattern rather than guessing.
        return new ExclusionRule(HttpVerb.ANY, requirePath(trimmed, rule));
    }

    private static HttpVerb parseVerb(String token, String original) {
        try {
            return HttpVerb.valueOf(token.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid exclusion rule '" + original + "': unrecognised HTTP verb '" + token + "'.", e);
        }
    }

    private static String requirePath(String path, String original) {
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException(
                    "Invalid exclusion rule '" + original + "': path pattern must start with '/'.");
        }
        return path;
    }
}
