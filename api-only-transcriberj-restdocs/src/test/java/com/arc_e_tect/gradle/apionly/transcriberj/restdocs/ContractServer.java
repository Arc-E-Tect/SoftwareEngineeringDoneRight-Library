package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A server with no framework: a JDK {@link HttpServer} that records every request it receives,
 * raw, and answers as its {@link Behaviour} says. What the generated tests are run against.
 */
final class ContractServer implements AutoCloseable {

    /**
     * A request, as it arrived.
     *
     * @param sequence the order it arrived in, among everything {@link Harness} records
     * @param method   the method
     * @param rawPath  the path, still percent-encoded
     * @param rawQuery the query, still percent-encoded, or null when there is none
     * @param headers  the headers, by lower-case name, each value as ISO 8859-1 text
     * @param body     the body's bytes; empty when there is none
     */
    record Received(long sequence, String method, String rawPath, String rawQuery, Map<String, List<String>> headers,
                    byte[] body) {

        /** The first value of a header, or null. */
        String header(String name) {
            List<String> values = headers.get(name.toLowerCase(java.util.Locale.ROOT));
            return values == null || values.isEmpty() ? null : values.get(0);
        }
    }

    /**
     * An answer.
     *
     * @param status      the status
     * @param contentType the content type, or null for none
     * @param body        the body, or null for none
     */
    record Answer(int status, String contentType, byte[] body) {
    }

    /** What the server answers a request with. */
    @FunctionalInterface
    interface Behaviour {
        Answer answer(Received request);
    }

    static {
        // TCP_NODELAY on the server's sockets. It writes a response's headers and body separately,
        // and without it Nagle's algorithm holds the body back for the client's delayed ACK: on
        // Linux, 40 ms a request, which made the tens of thousands of requests the mutant runs
        // send take half an hour. Read once, when the JDK server's configuration first loads.
        System.setProperty("sun.net.httpserver.nodelay", "true");
    }

    private final HttpServer server;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final List<Received> received = Collections.synchronizedList(new ArrayList<>());
    private volatile Behaviour behaviour;

    ContractServer(Behaviour behaviour) {
        this.behaviour = behaviour;
        try {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.createContext("/", this::handle);
        server.setExecutor(executor);
        server.start();
    }

    /** Where the server listens. */
    String baseUrl() {
        return "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort();
    }

    /** Answers from now on as {@code behaviour} says, and forgets what it received. */
    void behave(Behaviour behaviour) {
        this.behaviour = behaviour;
        received.clear();
    }

    /** Forgets what it received. */
    void clear() {
        received.clear();
    }

    /** Every request received since the behaviour was last set, or it was cleared, in order. */
    List<Received> received() {
        synchronized (received) {
            return List.copyOf(received);
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            byte[] body;
            try (InputStream in = exchange.getRequestBody()) {
                body = in.readAllBytes();
            }
            Map<String, List<String>> headers = new TreeMap<>();
            exchange.getRequestHeaders().forEach((name, values) ->
                    headers.put(name.toLowerCase(java.util.Locale.ROOT), List.copyOf(values)));
            Received request = new Received(Harness.next(), exchange.getRequestMethod(),
                    exchange.getRequestURI().getRawPath(), exchange.getRequestURI().getRawQuery(), headers, body);
            received.add(request);
            Answer answer;
            try {
                answer = behaviour.answer(request);
            } catch (RuntimeException e) {
                answer = new Answer(599, "text/plain", String.valueOf(e).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            if (answer.contentType() != null) exchange.getResponseHeaders().set("Content-Type", answer.contentType());
            byte[] out = answer.body() == null ? new byte[0] : answer.body();
            exchange.sendResponseHeaders(answer.status(), out.length == 0 ? -1 : out.length);
            if (out.length > 0) {
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(out);
                }
            }
        }
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }
}
