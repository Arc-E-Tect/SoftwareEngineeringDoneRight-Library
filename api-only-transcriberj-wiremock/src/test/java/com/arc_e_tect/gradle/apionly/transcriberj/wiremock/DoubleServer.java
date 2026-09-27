package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * A real, in-process WireMock server on a dynamic port -- loading a files directory's
 * {@code mappings/} and {@code __files/} as WireMock's working directory, when it is given one --
 * and a catch-all stub at the lowest priority that answers {@code 200} with a marker header, so
 * that a request no generated stub matches is told apart from one a stub answered.
 *
 * <p>The server works on a copy of the files directory: WireMock deletes the mapping files of its
 * working directory when its stubs are reset or removed.
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
     * @param status   the status it was answered with
     */
    record Served(String method, String url, String servedBy, int status) {
    }

    final WireMockServer server;
    private final Path root;

    /** A server with no stub but the catch-all. */
    DoubleServer() {
        this(null);
    }

    /** A server whose working directory is a copy of a files directory: its mappings loaded, its bodies read. */
    DoubleServer(Path files) {
        this(files, true);
    }

    /**
     * A server whose working directory is a copy of a files directory's bodies, or of all of it.
     *
     * @param files    the files directory, or null for none
     * @param mappings whether its mappings are loaded too
     */
    DoubleServer(Path files, boolean mappings) {
        WireMockConfiguration options = options().dynamicPort();
        root = files == null ? null : copy(files, mappings);
        if (root != null) options = options.usingFilesUnderDirectory(root.toString());
        server = new WireMockServer(options);
        server.start();
        catchAll();
    }

    /** A server that reads a files directory's bodies, and has no stub but the catch-all. */
    static DoubleServer bodiesOf(Path files) {
        return new DoubleServer(files, false);
    }

    /** No stub but the catch-all; every request forgotten. */
    void clear() {
        server.resetAll();
        catchAll();
    }

    private static Path copy(Path files, boolean mappings) {
        try {
            Path root = Files.createTempDirectory("wiremock-root");
            try (Stream<Path> walk = Files.walk(files)) {
                for (Path from : walk.toList()) {
                    String relative = files.relativize(from).toString().replace('\\', '/');
                    if (!mappings && relative.startsWith(WireMockEmitter.MAPPINGS)) continue;
                    Path to = root.resolve(relative);
                    if (Files.isDirectory(from)) Files.createDirectories(to);
                    else Files.copy(from, to);
                }
            }
            Files.createDirectories(root.resolve(WireMockEmitter.MAPPINGS));
            return root;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void catchAll() {
        server.stubFor(any(anyUrl()).withName(CATCH_ALL).atPriority(LOWEST)
                .willReturn(aResponse().withStatus(200).withHeader(MARKER, "true")));
    }

    /** Registers a mapping. */
    void stub(MappingBuilder mapping) {
        server.stubFor(mapping);
    }

    /** Registers a mapping read from a file. */
    void stub(StubMapping mapping) {
        server.addStubMapping(mapping);
    }

    /** Every stub but the catch-all, by name. */
    List<String> stubs() {
        return server.getStubMappings().stream().map(StubMapping::getName).filter(n -> !CATCH_ALL.equals(n)).sorted()
                .toList();
    }

    /** Every request received since the last reset, in the order received. */
    List<Served> journal() {
        // WireMock lists the most recent first.
        List<ServeEvent> events = new ArrayList<>(server.getAllServeEvents());
        Collections.reverse(events);
        List<Served> out = new ArrayList<>();
        for (ServeEvent e : events) {
            out.add(new Served(e.getRequest().getMethod().getName(), e.getRequest().getUrl(),
                    e.getStubMapping() == null ? null : e.getStubMapping().getName(),
                    e.getResponse() == null ? 0 : e.getResponse().getStatus()));
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
        if (root == null) return;
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path p : walk.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
