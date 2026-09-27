package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * What the concrete test classes {@link GeneratedSuite} compiles read and report to: where the
 * double is, how their hooks reach it, the headers their client adds, and every mapping the hooks
 * registered, in order.
 *
 * <p>Public, since those classes are compiled into a package of their own. One suite runs at a
 * time.
 */
public final class Harness {

    /** How the hooks register a case's mapping, in the {@code java} format. */
    public enum Client {
        /** {@code server.stubFor(mapping)}, on the in-process server. */
        SERVER,
        /** {@code WireMock.stubFor(mapping)}, the static client configured for the server. */
        STATIC,
        /** {@code wireMock.register(mapping)}, a client instance pointing at the server. */
        INSTANCE
    }

    /**
     * The one connector every test's client uses, so that connections are kept alive and reused
     * rather than exhausting the local ports.
     */
    private static final org.springframework.http.client.reactive.ClientHttpConnector CONNECTOR =
            new org.springframework.http.client.reactive.JdkClientHttpConnector();
    private static final List<String> ARRANGED = Collections.synchronizedList(new ArrayList<>());
    private static volatile WireMockServer server;
    private static volatile WireMock instance;
    private static volatile Client client = Client.SERVER;
    private static volatile String snippets;
    private static volatile Map<String, String> defaultHeaders = Map.of();

    private Harness() {
    }

    /** Sets up a run: the server the tests are sent to, how the hooks reach it, where snippets go, and extra headers. */
    static void configure(WireMockServer server, Client client, String snippets, Map<String, String> defaultHeaders) {
        Harness.server = server;
        Harness.client = client;
        Harness.snippets = snippets;
        Harness.defaultHeaders = defaultHeaders;
        Harness.instance = client == Client.INSTANCE ? new WireMock(server.port()) : null;
        if (client == Client.STATIC) WireMock.configureFor(server.port());
        ARRANGED.clear();
    }

    /**
     * Where the server listens.
     *
     * @return its base URL
     */
    public static String baseUrl() {
        return server.baseUrl();
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
     * The headers the tests' client adds to every request that does not set them itself: a
     * project's client defaults.
     *
     * @return the headers
     */
    public static Map<String, String> defaultHeaders() {
        return defaultHeaders;
    }

    /**
     * Registers a case's mapping as this run's client does, and records it.
     *
     * @param mapping the mapping
     */
    public static void register(MappingBuilder mapping) {
        switch (client) {
            case SERVER -> server.stubFor(mapping);
            case STATIC -> WireMock.stubFor(mapping);
            case INSTANCE -> instance.register(mapping);
        }
        ARRANGED.add(mapping.build().getName());
    }

    /** The name of every mapping the hooks registered since the run was set up, in order. */
    static List<String> arranged() {
        synchronized (ARRANGED) {
            return List.copyOf(ARRANGED);
        }
    }
}
