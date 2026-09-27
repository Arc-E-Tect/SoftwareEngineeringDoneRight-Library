package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The in-memory store behind the {@link ValidatingServer}: which resources exist, each named by
 * the path that addresses it, its template filled with its path values -- {@code /orders/abc} --
 * or, for an operation with no path parameter, by its template alone: what a {@code POST} to a
 * collection creates.
 *
 * <p>It is also the fixture interface the harness's concrete test classes use from
 * {@code arrangeState}: {@link #insert}, {@link #delete} and {@link #clear}, and
 * {@link #arrange}, the correct fixture, which does what the generated Javadoc says each kind
 * needs.
 */
final class Store {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{[^}]+}");

    private final Set<String> resources = ConcurrentHashMap.newKeySet();

    /** The name of the resource a path template and its values address. */
    static String key(String pathTemplate, List<String> pathValues) {
        StringBuilder out = new StringBuilder();
        Matcher m = PLACEHOLDER.matcher(pathTemplate);
        int last = 0;
        int i = 0;
        while (m.find()) {
            out.append(pathTemplate, last, m.start()).append(i < pathValues.size() ? pathValues.get(i) : "");
            last = m.end();
            i++;
        }
        return out.append(pathTemplate.substring(last)).toString();
    }

    /** Makes a resource exist. */
    void insert(String key) {
        resources.add(key);
    }

    /** Makes a resource not exist. */
    void delete(String key) {
        resources.remove(key);
    }

    /** Whether a resource exists. */
    boolean exists(String key) {
        return resources.contains(key);
    }

    /** Forgets every resource. */
    void clear() {
        resources.clear();
    }

    /** How many resources exist. */
    int size() {
        return resources.size();
    }

    /**
     * The correct fixture: for a not-found case, and a success case expecting {@code 201}, the
     * resource the request names does not exist; for any other success case, it does.
     */
    void arrange(Harness.Stateful c) {
        String key = key(c.pathTemplate(), c.pathValues());
        if (c.kind().equals("NOT_FOUND") || c.status() == 201) {
            delete(key);
        } else {
            insert(key);
        }
    }

    /** The wrong state for a case: the opposite of what {@link #arrange} does. */
    void misarrange(Harness.Stateful c) {
        String key = key(c.pathTemplate(), c.pathValues());
        if (c.kind().equals("NOT_FOUND") || c.status() == 201) {
            insert(key);
        } else {
            delete(key);
        }
    }
}
