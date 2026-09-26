package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What the concrete test classes {@link GeneratedSuite} compiles read and report to: where the
 * double is, how their fixture hook reaches it, and every case the hook was called with, in order.
 *
 * <p>Public, since those classes are compiled into a package of their own. One suite runs at a
 * time.
 */
public final class Harness {

    /** How the fixture hook registers its case's mapping. */
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
    private static volatile boolean defaultHeaders;

    private Harness() {
    }

    /**
     * Sets up a run: the server the tests are sent to, how the hook reaches it, where snippets go,
     * and whether the tests' client adds the reference implementation's default headers.
     */
    static void configure(WireMockServer server, Client client, String snippets, boolean defaultHeaders) {
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
     * Whether the tests' client adds the reference implementation's default headers.
     *
     * @return whether it does
     */
    public static boolean defaultHeaders() {
        return defaultHeaders;
    }

    /**
     * The in-process server.
     *
     * @return the server
     */
    public static WireMockServer server() {
        return server;
    }

    /**
     * The client instance pointing at the server, when the run uses one.
     *
     * @return the client
     */
    public static WireMock instance() {
        return instance;
    }

    /**
     * How the fixture hook registers its case's mapping in this run.
     *
     * @return the client
     */
    public static Client client() {
        return client;
    }

    /**
     * Records a call of the fixture hook.
     *
     * @param mappingName the name of the mapping it registered
     */
    public static void arranged(String mappingName) {
        ARRANGED.add(mappingName);
    }

    /** The name of every mapping the fixture hook registered since the run was set up, in order. */
    static List<String> arranged() {
        synchronized (ARRANGED) {
            return List.copyOf(ARRANGED);
        }
    }
}
