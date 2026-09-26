package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sends a request with the JDK {@code HttpClient}, independently of the REST Docs emitter's
 * rendered tests, encoded as those tests encode it: every path value and query name and value
 * percent-encoded as UTF-8 except the characters RFC 3986 leaves unreserved, headers and body as
 * they are.
 */
final class Replay {

    /**
     * A request, as the generated {@code ContractRequest} holds it.
     *
     * @param method         the method
     * @param pathTemplate   the path template
     * @param pathParameters the path values
     * @param query          the query parameters, each a name and a value
     * @param headers        the headers, each a name and a value
     * @param contentType    the content type, or null
     * @param body           the body's text, or null
     */
    record Request(String method, String pathTemplate, List<String> pathParameters, List<String[]> query,
                   List<String[]> headers, String contentType, String body) {

        /** The request a generated {@code ContractRequest} holds. */
        static Request of(Object contractRequest) {
            try {
                @SuppressWarnings("unchecked")
                List<String> path = (List<String>) invoke(contractRequest, "pathParameters");
                return new Request((String) invoke(contractRequest, "method"),
                        (String) invoke(contractRequest, "pathTemplate"), path,
                        pairs(invoke(contractRequest, "query")), pairs(invoke(contractRequest, "headers")),
                        (String) invoke(contractRequest, "contentType"), (String) invoke(contractRequest, "body"));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }

        private static List<String[]> pairs(Object pairs) throws ReflectiveOperationException {
            List<String[]> out = new ArrayList<>();
            for (Object pair : (List<?>) pairs) {
                out.add(new String[]{(String) invoke(pair, "name"), (String) invoke(pair, "value")});
            }
            return out;
        }
    }

    /**
     * A response.
     *
     * @param status      its status
     * @param contentType its {@code Content-Type}, or null
     * @param catchAll    whether the double's catch-all answered it
     * @param body        its body
     */
    record Response(int status, String contentType, boolean catchAll, String body) {
    }

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final String UNRESERVED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{[^}]+}");

    private Replay() {
    }

    /** The request of a generated {@code InvalidRequestCase}. */
    static Request of(Object invalid) {
        try {
            return Request.of(invoke(invalid, "request"));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Sends a request to a double. */
    static Response send(DoubleServer server, Request request) {
        return send(server, request, Map.of());
    }

    /** Sends a request to a double with extra headers, each added unless the request sets it itself. */
    static Response send(DoubleServer server, Request request, Map<String, String> extra) {
        StringBuilder url = new StringBuilder(server.baseUrl()).append(path(request));
        for (int i = 0; i < request.query().size(); i++) {
            url.append(i == 0 ? '?' : '&').append(encode(request.query().get(i)[0])).append('=')
                    .append(encode(request.query().get(i)[1]));
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url.toString())).method(request.method(),
                request.body() == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(request.body(), StandardCharsets.UTF_8));
        List<String> set = new ArrayList<>();
        for (String[] header : request.headers()) {
            builder.header(header[0], header[1]);
            set.add(header[0].toLowerCase(java.util.Locale.ROOT));
        }
        if (request.contentType() != null) {
            builder.header("Content-Type", request.contentType());
            set.add("content-type");
        }
        extra.forEach((name, value) -> {
            if (!set.contains(name.toLowerCase(java.util.Locale.ROOT))) builder.header(name, value);
        });
        try {
            HttpResponse<String> response = CLIENT.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Response(response.statusCode(), response.headers().firstValue("Content-Type").orElse(null),
                    response.headers().firstValue(DoubleServer.MARKER).isPresent(), response.body());
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /** The path as it travels: the template filled with the values, each encoded. */
    static String path(Request request) {
        Matcher placeholder = PLACEHOLDER.matcher(request.pathTemplate());
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (placeholder.find()) {
            placeholder.appendReplacement(out, Matcher.quoteReplacement(encode(request.pathParameters().get(i++))));
        }
        placeholder.appendTail(out);
        return out.toString();
    }

    /** A value percent-encoded as UTF-8, except the characters RFC 3986 leaves unreserved. */
    static String encode(String value) {
        StringBuilder out = new StringBuilder();
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            if (b >= 0 && UNRESERVED.indexOf(b) >= 0) {
                out.append((char) b);
            } else {
                out.append('%').append(String.format(java.util.Locale.ROOT, "%02X", b & 0xff));
            }
        }
        return out.toString();
    }

    private static Object invoke(Object target, String accessor) throws ReflectiveOperationException {
        return target.getClass().getMethod(accessor).invoke(target);
    }
}
