package com.arc_e_tect.gradle.detector.core.exclude;

import com.arc_e_tect.gradle.detector.core.Described;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;

import java.util.List;

/**
 * Matches a {@link Described} verb + path pair against {@link ExclusionRule}s, using Ant-style
 * path matching: a {@code *} segment matches exactly one path segment (literal or a
 * {@code {placeholder}}), and a trailing {@code **} segment matches any remaining number of
 * segments, including zero.
 */
public final class ExclusionMatcher {

    private ExclusionMatcher() {}

    /**
     * Returns whether any rule in {@code rules} matches {@code item}.
     *
     * @param item  the endpoint to test
     * @param rules the exclusion rules to test against
     * @return {@code true} when at least one rule matches
     */
    public static boolean matches(Described item, List<ExclusionRule> rules) {
        for (ExclusionRule rule : rules) {
            if (matches(item, rule)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether {@code rule} matches {@code item}: {@code rule}'s verb is
     * {@link HttpVerb#ANY} or equal to {@code item}'s verb, and {@code rule}'s path pattern
     * matches {@code item}'s path.
     *
     * @param item the endpoint to test
     * @param rule the rule to test against
     * @return {@code true} when {@code rule} matches {@code item}
     */
    public static boolean matches(Described item, ExclusionRule rule) {
        boolean verbMatches = rule.verb() == HttpVerb.ANY || rule.verb() == item.verb();
        return verbMatches && matchesPath(item.path(), rule.pathPattern());
    }

    private static boolean matchesPath(String path, String pattern) {
        String[] pathSegments = segments(path);
        String[] patternSegments = segments(pattern);
        int i = 0;
        for (; i < patternSegments.length; i++) {
            String patternSegment = patternSegments[i];
            if ("**".equals(patternSegment)) {
                return true;
            }
            if (i >= pathSegments.length) {
                return false;
            }
            if (!"*".equals(patternSegment) && !patternSegment.equals(pathSegments[i])) {
                return false;
            }
        }
        return i == pathSegments.length;
    }

    private static String[] segments(String path) {
        if ("/".equals(path)) {
            return new String[0];
        }
        String trimmed = path.startsWith("/") ? path.substring(1) : path;
        return trimmed.split("/");
    }
}
