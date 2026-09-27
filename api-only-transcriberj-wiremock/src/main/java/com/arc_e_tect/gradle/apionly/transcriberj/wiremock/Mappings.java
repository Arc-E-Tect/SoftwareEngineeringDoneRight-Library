package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import com.arc_e_tect.gradle.apionly.transcriberj.spi.CaseKind;
import com.arc_e_tect.gradle.apionly.transcriberj.spi.ContractCase;
import com.arc_e_tect.gradle.apionly.transcriberj.spi.ContractRequest;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The stubs as WireMock mapping documents: what {@code files} writes, and what the {@code java}
 * format's mappings serialise to. Written from WireMock's mapping format without WireMock, which
 * must not be loaded into Gradle's JVM; the tests hold them to WireMock's own mapper.
 */
final class Mappings {

    private static final String UNRESERVED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{[^}]+}");

    /** Where a mapping's response body comes from. */
    interface Bodies {

        /**
         * How a mapping refers to a body class's full body.
         *
         * @param bodyClass the class
         * @return the response members that give the body, such as {@code bodyFileName}
         */
        Map<String, Object> body(String bodyClass);
    }

    private Mappings() {
    }

    /**
     * A case's exact stub: its request, matched exactly, answered as the contract declares.
     *
     * @param plan   the plan
     * @param op     the case's operation
     * @param c      the case
     * @param bodies where its response body comes from
     * @return the mapping
     */
    static Map<String, Object> exact(Plan plan, Plan.Op op, ContractCase c, Bodies bodies) {
        ContractRequest request = c.request();
        Map<String, Object> match = new LinkedHashMap<>();
        match.put("method", request.method());
        match.put("urlPath", path(request));

        Map<String, List<String>> sent = new LinkedHashMap<>();
        for (ContractRequest.Pair p : request.query()) sent.computeIfAbsent(p.name(), n -> new ArrayList<>()).add(p.value());
        Map<String, Object> query = new LinkedHashMap<>();
        sent.forEach((name, values) -> query.put(name, Map.of("hasExactly",
                values.stream().map(v -> (Object) Map.of("equalTo", v)).toList())));
        for (String name : c.declaredQuery()) {
            if (!sent.containsKey(name)) query.put(name, absent());
        }
        if (!query.isEmpty()) match.put("queryParameters", query);

        Map<String, Object> headers = new LinkedHashMap<>();
        List<String> names = new ArrayList<>();
        for (ContractRequest.Pair h : request.headers()) {
            names.add(h.name());
            if (!h.name().equalsIgnoreCase("Accept")) headers.put(h.name(), Map.of("equalTo", h.value()));
        }
        for (String name : c.declaredHeaders()) {
            if (names.stream().noneMatch(name::equalsIgnoreCase)) headers.put(name, absent());
        }
        if (op.acceptable() != null) {
            headers.put("Accept", c.kind() == CaseKind.NOT_ACCEPTABLE ? unacceptable(op.acceptable())
                    : absentOr(op.acceptable()));
        }
        if (request.body() != null) headers.put("Content-Type", Map.of("matches", Patterns.contentType(request.contentType())));
        if (!headers.isEmpty()) match.put("headers", headers);

        if (request.body() == null) {
            match.put("bodyPatterns", List.of(absent()));
        } else {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("equalToJson", request.body());
            json.put("ignoreArrayOrder", false);
            json.put("ignoreExtraElements", false);
            match.put("bodyPatterns", List.of(json));
        }
        Map<String, Object> response = response(c.expectedStatus(),
                c.expectedContentTypes().isEmpty() ? null : c.expectedContentTypes().get(0),
                c.expectedContentTypes().isEmpty() ? null : c.responseBodyClass(), bodies);
        return mapping(op.name() + "/" + c.id(), plan.priority(), match, response,
                metadata(plan, op, c.id(), c.kind().name(), false, null));
    }

    /**
     * One of an operation's fallbacks.
     *
     * @param plan     the plan
     * @param op       the operation
     * @param fallback which fallback
     * @param bodies   where its response body comes from
     * @return the mapping
     */
    static Map<String, Object> fallback(Plan plan, Plan.Op op, Plan.Fallback fallback, Bodies bodies) {
        Map<String, Object> match = new LinkedHashMap<>();
        match.put("method", op.method());
        match.put("urlPathTemplate", op.pathTemplate());
        Map<String, Object> headers = new LinkedHashMap<>();
        switch (fallback) {
            case NOT_ACCEPTABLE -> headers.put("Accept", unacceptable(op.acceptable()));
            case UNSUPPORTED_MEDIA_TYPE -> headers.put("Content-Type", unacceptable(op.supported()));
            case VALID -> valid(op, match, headers);
            case INVALID -> {
                // The method and path template alone: every other request to the operation.
            }
        }
        if (!headers.isEmpty()) match.put("headers", headers);
        if (fallback == Plan.Fallback.VALID && op.bodySchema() != null) {
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("matchesJsonSchema", op.bodySchema());
            schema.put("schemaVersion", "V202012");
            match.put("bodyPatterns", List.of(op.bodyRequired() ? schema : Map.of("or", List.of(absent(), schema))));
        }
        Plan.Answer answer = op.answers().get(fallback);
        Map<String, Object> response = response(answer.status(), answer.contentType(), answer.bodyClass(), bodies);
        return mapping(op.name() + "/" + fallback.caseId(), op.priority(plan.priority(), fallback), match, response,
                metadata(plan, op, fallback.caseId(), fallback.kind, true, fallback));
    }

    /** The valid fallback's parameters and headers. */
    private static void valid(Plan.Op op, Map<String, Object> match, Map<String, Object> headers) {
        Map<String, Object> path = new LinkedHashMap<>();
        Map<String, Object> query = new LinkedHashMap<>();
        for (Patterns.Parameter p : op.parameters()) {
            Object pattern = parameter(p);
            if (pattern == null) continue;
            switch (p.in()) {
                case "path" -> path.put(p.name(), pattern);
                case "query" -> query.put(p.name(), pattern);
                default -> headers.put(p.name(), pattern);
            }
        }
        if (!path.isEmpty()) match.put("pathParameters", path);
        if (!query.isEmpty()) match.put("queryParameters", query);
        if (op.acceptable() != null) headers.put("Accept", absentOr(op.acceptable()));
        if (op.bodyMediaType() != null) {
            String contentType = Patterns.contentType(op.bodyMediaType());
            headers.put("Content-Type", op.bodyRequired() ? Map.of("matches", contentType) : absentOr(contentType));
        }
    }

    /** What a fallback matches a parameter with, or {@code null} when it checks nothing of it. */
    private static Object parameter(Patterns.Parameter p) {
        if (p.pattern() == null) return p.required() ? Map.of("matches", Patterns.PRESENT) : null;
        Map<String, Object> matches = Map.of("matches", p.pattern());
        return p.required() || p.in().equals("path") ? matches : Map.of("or", List.of(absent(), matches));
    }

    private static Map<String, Object> response(int status, String contentType, String bodyClass, Bodies bodies) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", status);
        if (contentType != null) {
            response.put("headers", Map.of("Content-Type", contentType));
            if (bodyClass != null) response.putAll(bodies.body(bodyClass));
        }
        return response;
    }

    private static Map<String, Object> mapping(String name, int priority, Map<String, Object> request,
                                               Map<String, Object> response, Map<String, Object> metadata) {
        Map<String, Object> mapping = new LinkedHashMap<>();
        mapping.put("name", name);
        mapping.put("priority", priority);
        mapping.put("request", request);
        mapping.put("response", response);
        mapping.put("metadata", metadata);
        return mapping;
    }

    /**
     * A stub's metadata: the contract and version, the operation, the case -- or the fallback --
     * and its kind, whether it is a fallback, and, for the valid one, what it enforces and does not.
     */
    static Map<String, Object> metadata(Plan plan, Plan.Op op, String caseId, String kind, boolean fallback,
                                        Plan.Fallback which) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("contract", plan.contract());
        metadata.put("contractVersion", plan.contractVersion());
        metadata.put("operation", op.name());
        metadata.put("case", caseId);
        metadata.put("kind", kind);
        metadata.put("fallback", fallback);
        if (which == Plan.Fallback.VALID) {
            metadata.put("enforces", op.enforces());
            metadata.put("doesNotEnforce", op.doesNotEnforce());
        }
        return metadata;
    }

    private static Map<String, Object> absent() {
        return Map.of("absent", true);
    }

    private static Map<String, Object> absentOr(String pattern) {
        return Map.of("or", List.of(absent(), Map.of("matches", pattern)));
    }

    /** Present, and not matching: {@code not} alone would also match an absent header. */
    private static Map<String, Object> unacceptable(String pattern) {
        return Map.of("and", List.of(Map.of("matches", Patterns.PRESENT), Map.of("not", Map.of("matches", pattern))));
    }

    /** The path as the request travels: the template filled with the values, each percent-encoded. */
    static String path(ContractRequest request) {
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
}
