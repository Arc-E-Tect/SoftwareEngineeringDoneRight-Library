package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * What the concrete test classes {@link GeneratedSuite} compiles read and report to: where the
 * server is, where snippets go, the documentation prefix to use, and every call of the fixture
 * hook, numbered in the same sequence as the requests the server receives.
 *
 * <p>Public, since those classes are compiled into a package of their own. One suite runs at a
 * time.
 */
public final class Harness {

    /**
     * One call of {@code arrangeInvalidRequest}.
     *
     * @param sequence  when, in the sequence the server's requests are numbered in
     * @param testClass the concrete class's simple name
     * @param caseId    the id of the case it was called with
     */
    public record Arrangement(long sequence, String testClass, String caseId) {
    }

    private static final AtomicLong SEQUENCE = new AtomicLong();
    /**
     * One connector for every test, so that connections are kept alive and reused: a client per
     * test would open a connection per request, and tens of thousands of them exhaust the local
     * ports while they wait to close.
     */
    private static final org.springframework.http.client.reactive.ClientHttpConnector CONNECTOR =
            new org.springframework.http.client.reactive.JdkClientHttpConnector();
    private static final List<Arrangement> ARRANGEMENTS = Collections.synchronizedList(new ArrayList<>());
    private static volatile String baseUrl;
    private static volatile String snippets;
    private static volatile String prefix;

    private Harness() {
    }

    /** Sets up a run: the server, the snippet directory, and a prefix to override with, or null for none. */
    static void configure(String baseUrl, String snippets, String prefix) {
        Harness.baseUrl = baseUrl;
        Harness.snippets = snippets;
        Harness.prefix = prefix;
        ARRANGEMENTS.clear();
    }

    /**
     * Where the server listens.
     *
     * @return its base URL
     */
    public static String baseUrl() {
        return baseUrl;
    }

    /**
     * The connector every test's client uses.
     *
     * @return the connector
     */
    public static org.springframework.http.client.reactive.ClientHttpConnector connector() {
        return CONNECTOR;
    }

    /**
     * Where the snippets go.
     *
     * @return the directory
     */
    public static String snippets() {
        return snippets;
    }

    /**
     * The documentation prefix to override the default with.
     *
     * @return the prefix, or null to keep the default
     */
    public static String prefix() {
        return prefix;
    }

    /**
     * Records a call of the fixture hook.
     *
     * @param testClass the concrete class
     * @param caseId    the case's id
     */
    public static void arranged(String testClass, String caseId) {
        ARRANGEMENTS.add(new Arrangement(next(), testClass, caseId));
    }

    /** Every call of the fixture hook since the run was set up. */
    static List<Arrangement> arrangements() {
        synchronized (ARRANGEMENTS) {
            return List.copyOf(ARRANGEMENTS);
        }
    }

    /** The next number in the sequence. */
    static long next() {
        return SEQUENCE.incrementAndGet();
    }
}
