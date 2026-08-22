package com.arc_e_tect.gradle.detector.core.exclude;

import com.arc_e_tect.gradle.detector.core.Described;

import java.util.ArrayList;
import java.util.List;

/**
 * Partitions a list of {@link Described} endpoints by whether they match any of a list of
 * {@link ExclusionRule}s - shared by Shadow, Mirage, and Doppelganger API Detector so each
 * plugin's own task applies exclusion identically: split a finder's ground-truth list into what's
 * still reportable/fails the build and what's excluded, and strip excluded endpoints out of
 * whatever is passed to {@code ContractHistoryUpdater}.
 */
public final class ExclusionFilter {

    private ExclusionFilter() {}

    /**
     * Returns every item in {@code items} that matches none of {@code rules}.
     *
     * @param items the items to filter
     * @param rules the exclusion rules
     * @param <T>   the item type
     * @return the non-matching items, in {@code items}' order
     */
    public static <T extends Described> List<T> excludeMatching(List<T> items, List<ExclusionRule> rules) {
        if (rules.isEmpty()) {
            return items;
        }
        List<T> result = new ArrayList<>();
        for (T item : items) {
            if (!ExclusionMatcher.matches(item, rules)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Returns every item in {@code items} that matches at least one of {@code rules}.
     *
     * @param items the items to filter
     * @param rules the exclusion rules
     * @param <T>   the item type
     * @return the matching items, in {@code items}' order
     */
    public static <T extends Described> List<T> onlyMatching(List<T> items, List<ExclusionRule> rules) {
        if (rules.isEmpty()) {
            return List.of();
        }
        List<T> result = new ArrayList<>();
        for (T item : items) {
            if (ExclusionMatcher.matches(item, rules)) {
                result.add(item);
            }
        }
        return result;
    }
}
