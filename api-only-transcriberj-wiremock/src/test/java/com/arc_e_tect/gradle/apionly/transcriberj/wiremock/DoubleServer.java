package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * A real, in-process WireMock server on a dynamic port, carrying one catch-all stub at the lowest
 * priority that answers {@code 200} with a marker header. A request no generated stub matches
 * reaches the catch-all, so the generated test fails with its {@code 2xx} message rather than for
 * WireMock's {@code 404}, which would read as "looked the resource up first".
 */
final class DoubleServer implements AutoCloseable {

    /** The catch-all's name, as the journal names the stub that served a request. */
    static final String CATCH_ALL = "catch-all";

    /** The header the catch-all marks its responses with. */
    static final String MARKER = "X-Catch-All";

    /** WireMock's lowest priority is the highest number; nothing generated or hand-written uses this one. */
    private static final int LOWEST = Integer.MAX_VALUE;

    /**
     * One request the server received, and which stub answered it.
     *
     * @param method   its method
     * @param url      its path and query, as they travelled
     * @param servedBy the name of the stub that answered, or null for an unnamed one
     */
    record Served(String method, String url, String servedBy) {
    }

    final WireMockServer server;

    DoubleServer() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        reset();
    }

    /** Forgets every stub and request, leaving the catch-all. */
    void reset() {
        server.resetAll();
        server.stubFor(any(anyUrl()).withName(CATCH_ALL).atPriority(LOWEST)
                .willReturn(aResponse().withStatus(200).withHeader(MARKER, "true")));
    }

    /** Registers a mapping. */
    void stub(MappingBuilder mapping) {
        server.stubFor(mapping);
    }

    /** Every request received since the last reset, in the order received. */
    List<Served> journal() {
        // WireMock lists the most recent first.
        List<ServeEvent> events = new ArrayList<>(server.getAllServeEvents());
        Collections.reverse(events);
        List<Served> out = new ArrayList<>();
        for (ServeEvent e : events) {
            out.add(new Served(e.getRequest().getMethod().getName(), e.getRequest().getUrl(),
                    e.getStubMapping() == null ? null : e.getStubMapping().getName()));
        }
        return out;
    }

    /** Forgets the requests received, keeping the stubs. */
    void forgetRequests() {
        server.resetRequests();
    }

    String baseUrl() {
        return server.baseUrl();
    }

    @Override
    public void close() {
        server.stop();
    }
}
