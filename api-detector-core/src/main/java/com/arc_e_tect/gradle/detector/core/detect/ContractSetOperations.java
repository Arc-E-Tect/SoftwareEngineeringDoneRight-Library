package com.arc_e_tect.gradle.detector.core.detect;

import com.arc_e_tect.gradle.detector.core.Described;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.model.PathMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic, bidirectional set operations between two lists of {@link Described} verb + path
 * pairs, matched by {@link PathMatcher} and by verb - honoring {@link HttpVerb#ANY} on whichever
 * side represents a controller-scanned endpoint, since only that side can ever carry it (every
 * OpenAPI operation is described under one concrete verb).
 */
public final class ContractSetOperations {

    private ContractSetOperations() {}

    /**
     * Returns every item in {@code left} that has no matching verb + path in {@code right}.
     *
     * @param left  the items to filter
     * @param right the items to match against
     * @param <T>   the type of item returned, preserved from {@code left}
     * @return the items in {@code left} with no match in {@code right}, in {@code left}'s order
     */
    public static <T extends Described> List<T> difference(List<T> left, List<? extends Described> right) {
        List<T> result = new ArrayList<>();
        for (T item : left) {
            if (!hasMatch(item, right)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Returns every item in {@code left} that has a matching verb + path in {@code right}.
     *
     * @param left  the items to filter
     * @param right the items to match against
     * @param <T>   the type of item returned, preserved from {@code left}
     * @return the items in {@code left} with a match in {@code right}, in {@code left}'s order
     */
    public static <T extends Described> List<T> intersection(List<T> left, List<? extends Described> right) {
        List<T> result = new ArrayList<>();
        for (T item : left) {
            if (hasMatch(item, right)) {
                result.add(item);
            }
        }
        return result;
    }

    private static boolean hasMatch(Described item, List<? extends Described> candidates) {
        return candidates.stream().anyMatch(candidate -> matches(item, candidate));
    }

    private static boolean matches(Described a, Described b) {
        return (a.verb() == HttpVerb.ANY || b.verb() == HttpVerb.ANY || a.verb() == b.verb())
                && PathMatcher.matches(a.path(), b.path());
    }
}
