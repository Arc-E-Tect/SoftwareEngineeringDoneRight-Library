package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A server that implements the contract faithfully, as a real one would, with the independent
 * validator and an in-memory {@link Store}. For every request, in this order:
 *
 * <ol>
 *   <li>the operation is found by its method and path, or {@code 404};</li>
 *   <li>a body whose {@code Content-Type} none of the request body's media types matches is
 *   refused with {@code 415};</li>
 *   <li>an {@code Accept} no response's media type matches is refused with {@code 406};</li>
 *   <li>the request is validated against the contract -- parameters converted from the strings
 *   they travel as, the body checked strictly when strictness is on -- and any violation is
 *   answered with the invalid-request status;</li>
 *   <li>the store decides the success: a resource that exists is read, updated or deleted, and
 *   answered with the operation's first declared {@code 2xx} other than {@code 201}; one that
 *   does not is created, where the operation declares {@code 201}, or is {@code 404}.</li>
 * </ol>
 *
 * <p>Every answer carries the declared response's first content type and a body from its
 * response class's generated {@code fullBody()}. Header values are read as ISO 8859-1, as HTTP
 * defines them. It knows nothing of the cases, but for the variants the mutant tests use, each
 * changing one thing: see the methods that return a changed copy.
 */
final class ValidatingServer implements ContractServer.Behaviour {

    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{[^}]+}");

    private final GeneratedSuite suite;
    private final RequestValidation validation;
    final Store store;

    // The mutations: each null or false in the faithful server.
    private JsonNode target;
    private Function<ContractServer.Received, ContractServer.Answer> targetAnswer;
    private boolean ignoringAccept;
    private boolean anyContentType;
    private String rejectedLocation;
    private String rejectedPointer;
    private String substitutedLocation;
    private int substitutedFrom;
    private int substitutedTo;
    private String foundLocation;

    private ValidatingServer(GeneratedSuite suite, Store store) {
        this.suite = suite;
        this.validation = new RequestValidation(suite.oracle, suite.settings.strictRequests());
        this.store = store;
    }

    /** The faithful server, with a store of its own. */
    static ValidatingServer of(GeneratedSuite suite) {
        return new ValidatingServer(suite, new Store());
    }

    /** The faithful server, over a store. */
    static ValidatingServer of(GeneratedSuite suite, Store store) {
        return new ValidatingServer(suite, store);
    }

    /** The server that does not enforce one invalid-request case's keyword at its location, and accepts what only breaks that. */
    static ValidatingServer ignoring(GeneratedSuite suite, JsonNode c) {
        return answering(suite, c, r -> accepted());
    }

    /**
     * The server that answers one case's request with something else: for an invalid request, any
     * request whose only violation is the case's; for any other kind, the case's request exactly.
     */
    static ValidatingServer answering(GeneratedSuite suite, JsonNode c,
                                      Function<ContractServer.Received, ContractServer.Answer> answer) {
        ValidatingServer out = of(suite);
        out.target = c;
        out.targetAnswer = answer;
        return out;
    }

    /**
     * This server, ignoring {@code Accept}: a request whose {@code Accept} it should refuse is
     * served, with the operation's first declared {@code 2xx}, whatever the store holds.
     */
    ValidatingServer ignoringAccept() {
        ignoringAccept = true;
        return this;
    }

    /**
     * This server, accepting any {@code Content-Type}: a body it should refuse is served, with the
     * operation's first declared {@code 2xx}, whatever the store holds.
     */
    ValidatingServer acceptingAnyContentType() {
        anyContentType = true;
        return this;
    }

    /** This server, rejecting a body of one operation that holds the member at a pointer, as if it were invalid. */
    ValidatingServer rejectingMember(String location, String pointer) {
        rejectedLocation = location;
        rejectedPointer = pointer;
        return this;
    }

    /** This server, answering one operation's success with another status. */
    ValidatingServer substituting(String location, int from, int to) {
        substitutedLocation = location;
        substitutedFrom = from;
        substitutedTo = to;
        return this;
    }

    /** This server, answering one operation's request for a missing resource as if it existed. */
    ValidatingServer findingMissing(String location) {
        foundLocation = location;
        return this;
    }

    /**
     * A request, as the validator reads one.
     *
     * @param operation   the operation's entry in the report
     * @param request     the request, shaped as the report records one
     * @param contentType the request's media type, without parameters, or null
     */
    record Parsed(JsonNode operation, ObjectNode request, String contentType) {
    }

    @Override
    public ContractServer.Answer answer(ContractServer.Received received) {
        Parsed parsed = parse(received);
        if (parsed == null) return new ContractServer.Answer(404, null, null);
        JsonNode operation = parsed.operation();
        String location = operation.get("location").stringValue();
        if (target != null && !invalidRequest(target) && sameRequest(target, parsed, received)) {
            return targetAnswer.apply(received);
        }
        if (received.body().length > 0 && parsed.contentType() != null) {
            boolean supported = requestMediaTypes(location).stream().anyMatch(d -> matches(d, parsed.contentType()));
            if (!supported) return anyContentType ? respond(operation, firstSuccess(location)) : respond(operation, 415);
        }
        if (!acceptable(location, received.header("accept"))) {
            return ignoringAccept ? respond(operation, firstSuccess(location)) : respond(operation, 406);
        }
        List<RequestValidation.Problem> problems = parsed.request() == null
                ? List.of(new RequestValidation.Problem("body", null, "json", "", null))
                : validation.problems(parsed.request());
        if (target != null && invalidRequest(target) && !problems.isEmpty()) {
            if (without(problems, target, parsed).isEmpty()) return targetAnswer.apply(received);
        }
        int invalid = Integer.parseInt(suite.settings.invalidRequestStatus());
        if (!problems.isEmpty()) return respond(operation, invalid);
        if (location.equals(rejectedLocation) && !parsed.request().path("body").at(rejectedPointer).isMissingNode()) {
            return respond(operation, invalid);
        }
        int status = state(operation, location, parsed.request());
        if (location.equals(substitutedLocation) && status == substitutedFrom) status = substitutedTo;
        return respond(operation, status);
    }

    /** An operation's first declared {@code 2xx}, or {@code 200}. */
    private int firstSuccess(String location) {
        for (String s : suite.oracle.document.at(location + "/responses").propertyNames()) {
            if (s.matches("2[0-9][0-9]")) return Integer.parseInt(s);
        }
        return 200;
    }

    /** What the store makes of a valid request: its status, having created or deleted what it does. */
    private int state(JsonNode operation, String location, ObjectNode request) {
        List<Integer> success = new ArrayList<>();
        suite.oracle.document.at(location + "/responses").propertyNames().forEach(s -> {
            if (s.matches("2[0-9][0-9]")) success.add(Integer.parseInt(s));
        });
        Integer other = success.stream().filter(s -> s != 201).findFirst().orElse(null);
        boolean creates = success.contains(201);
        String template = operation.get("pathTemplate").stringValue();
        if (!template.contains("{") && !creates) return other == null ? 200 : other;
        List<String> values = new ArrayList<>();
        request.get("pathParameters").forEach(v -> values.add(v.stringValue()));
        String key = Store.key(template, values);
        if (store.exists(key)) {
            if (operation.get("method").stringValue().equalsIgnoreCase("DELETE")) store.delete(key);
            return other == null ? 409 : other;
        }
        if (creates) {
            store.insert(key);
            return 201;
        }
        if (location.equals(foundLocation)) return other == null ? 200 : other;
        return 404;
    }

    /** {@code 200}, with an empty JSON object. */
    static ContractServer.Answer accepted() {
        return new ContractServer.Answer(200, "application/json", "{}".getBytes(StandardCharsets.UTF_8));
    }

    /** The response the contract declares for a status of an operation: its first content type, and a full body. */
    ContractServer.Answer respond(JsonNode operation, int status) {
        JsonNode response = suite.oracle.resolve(operation.get("location").stringValue() + "/responses/" + status);
        List<String> contentTypes = new ArrayList<>(response.path("content").propertyNames());
        if (contentTypes.isEmpty()) return new ContractServer.Answer(status, null, null);
        return new ContractServer.Answer(status, contentTypes.get(0), fullBody(operation, status));
    }

    /** The content types an operation's response of a status is declared with. */
    List<String> declaredContentTypes(JsonNode operation, int status) {
        JsonNode response = suite.oracle.resolve(operation.get("location").stringValue() + "/responses/" + status);
        return new ArrayList<>(response.path("content").propertyNames());
    }

    /** A valid body of an operation's response: its class's {@code fullBody()}, or {@code requiredBody()}. */
    byte[] fullBody(JsonNode operation, int status) {
        String bodyClass = null;
        for (JsonNode c : operation.get("cases")) {
            if (c.get("expectedStatus").asInt() == status && !c.get("responseBodyClass").isNull()) {
                bodyClass = c.get("responseBodyClass").stringValue();
            }
        }
        if (bodyClass == null) return "{}".getBytes(StandardCharsets.UTF_8);
        Class<?> type = suite.type(suite.settings.basePackage() + "." + bodyClass);
        for (String method : List.of("fullBody", "requiredBody")) {
            try {
                return ((String) type.getMethod(method).invoke(null)).getBytes(StandardCharsets.UTF_8);
            } catch (ReflectiveOperationException | RuntimeException e) {
                // No valid value of this variant: try the next.
            }
        }
        return "{}".getBytes(StandardCharsets.UTF_8);
    }

    /** The media types an operation's request body is declared with. */
    private List<String> requestMediaTypes(String location) {
        JsonNode body = suite.oracle.resolve(location + "/requestBody");
        return new ArrayList<>(body.path("content").propertyNames());
    }

    /** Whether an {@code Accept} admits a media type any of an operation's responses is declared with. */
    private boolean acceptable(String location, String accept) {
        if (accept == null) return true;
        List<String> offered = new ArrayList<>();
        for (String status : suite.oracle.document.at(location + "/responses").propertyNames()) {
            offered.addAll(suite.oracle.resolve(location + "/responses/" + status).path("content").propertyNames());
        }
        if (offered.isEmpty()) return true;
        for (String range : accept.split(",")) {
            for (String type : offered) {
                if (matches(range, type) || matches(type, range)) return true;
            }
        }
        return false;
    }

    /** Whether a media type or range matches a media type: parameters and case ignored. */
    static boolean matches(String range, String type) {
        String[] r = bare(range).split("/", 2);
        String[] t = bare(type).split("/", 2);
        if (r.length < 2 || t.length < 2) return false;
        if (r[0].equals("*")) return true;
        return r[0].equals(t[0]) && (r[1].equals("*") || r[1].equals(t[1]));
    }

    private static String bare(String mediaType) {
        int semicolon = mediaType.indexOf(';');
        return (semicolon < 0 ? mediaType : mediaType.substring(0, semicolon)).strip().toLowerCase(Locale.ROOT);
    }

    private static boolean invalidRequest(JsonNode c) {
        return c.get("kind").stringValue().equals("INVALID_REQUEST");
    }

    /** Whether a request is exactly a case's: path values, query, content type, body and {@code Accept}. */
    private static boolean sameRequest(JsonNode c, Parsed parsed, ContractServer.Received received) {
        JsonNode expected = c.get("request");
        if (parsed.request() == null) return false;
        if (!expected.get("method").stringValue().equals(parsed.operation().get("method").stringValue())
                || !expected.get("pathTemplate").stringValue().equals(parsed.operation().get("pathTemplate").stringValue())
                || !expected.get("pathParameters").equals(parsed.request().get("pathParameters"))
                || !expected.get("query").equals(parsed.request().get("query"))) {
            return false;
        }
        String contentType = expected.get("contentType").isNull() ? null : expected.get("contentType").stringValue();
        if (contentType == null ? parsed.contentType() != null : !contentType.equals(parsed.contentType())) {
            return false;
        }
        if (!expected.get("body").equals(parsed.request().get("body"))) return false;
        String accept = null;
        for (JsonNode h : expected.get("headers")) {
            if (h.get("name").stringValue().equalsIgnoreCase("Accept")) accept = h.get("value").stringValue();
        }
        String sent = received.header("accept");
        return accept == null ? sent == null : accept.equals(sent);
    }

    /** The problems without those that are one case's violation. */
    static List<RequestValidation.Problem> without(List<RequestValidation.Problem> problems, JsonNode c,
                                                   Parsed parsed) {
        return problems.stream().filter(p -> !matches(c, p, parsed) && !droppedAnnotation(c, p)).toList();
    }

    /**
     * Whether a problem is a case's violation: in the case's operation, its keyword where its fault is, with a {@code null}
     * case's value {@code null} and a type case's not, and a body case's in the same media type.
     */
    static boolean matches(JsonNode c, RequestValidation.Problem p, Parsed parsed) {
        JsonNode request = c.get("request");
        if (!request.get("method").stringValue().equals(parsed.operation().get("method").stringValue())
                || !request.get("pathTemplate").stringValue()
                .equals(parsed.operation().get("pathTemplate").stringValue())) {
            return false;
        }
        String keyword = c.get("keyword").stringValue();
        String in = c.get("in").stringValue();
        if (!p.in().equals(in)) return false;
        if (!in.equals("body")) {
            return p.name().equals(c.get("name").stringValue()) && p.keyword().equals(keyword);
        }
        JsonNode requestContentType = c.get("request").get("contentType");
        String expected = requestContentType.isNull() ? null : requestContentType.stringValue();
        if (expected != null && !expected.equals(parsed.contentType())) return false;
        String pointer = c.get("pointer").stringValue();
        if (keyword.equals("required") || keyword.equals("additionalProperties")) {
            if (pointer.isEmpty()) {
                // No body: a required member's problem is at the same pointer, but names the member.
                return p.keyword().equals("required") && p.pointer().isEmpty() && p.property() == null;
            }
            boolean kind = keyword.equals("required") ? p.keyword().equals("required")
                    : p.keyword().equals("additionalProperties") || p.keyword().equals("unevaluatedProperties");
            if (!kind) return false;
            String member = pointer.substring(pointer.lastIndexOf('/') + 1).replace("~1", "/").replace("~0", "~");
            return p.pointer().equals(pointer) || p.pointer().equals(pointer.substring(0, pointer.lastIndexOf('/')))
                    && member.equals(p.property());
        }
        if (!p.keyword().equals(keyword) || !p.pointer().equals(pointer)) return false;
        if (keyword.equals("type") && parsed.request() != null) {
            boolean isNull = parsed.request().get("body").at(pointer).isNull();
            return isNull == c.get("id").stringValue().endsWith("-null");
        }
        return true;
    }

    /**
     * Whether a problem is only the artefact of validating against the strictified document: a
     * member whose value breaks a keyword inside an {@code allOf} branch is also reported as unknown.
     */
    private static boolean droppedAnnotation(JsonNode c, RequestValidation.Problem p) {
        if (!c.get("in").stringValue().equals("body") || c.get("pointer").isNull()) return false;
        String pointer = c.get("pointer").stringValue();
        return p.keyword().equals("unevaluatedProperties") && p.property() != null
                && !c.get("keyword").stringValue().equals("additionalProperties")
                && (pointer + "/").startsWith(p.pointer() + "/" + RequestValidation.escape(p.property()) + "/");
    }

    /**
     * A request as received, shaped as the report records one: the operation found by its method
     * and path, every value decoded as a server decodes it -- a query's {@code +} as a space, as
     * servlet containers do -- and the body parsed. Null when no operation is at that path; a
     * {@code Parsed} without a request when the body is not JSON.
     */
    Parsed parse(ContractServer.Received received) {
        for (JsonNode operation : suite.report.get("contractCases")) {
            if (!operation.get("method").stringValue().equalsIgnoreCase(received.method())) continue;
            String template = operation.get("pathTemplate").stringValue();
            Matcher m = pattern(template).matcher(received.rawPath());
            if (!m.matches()) continue;
            ObjectNode request = NODES.objectNode();
            request.put("method", operation.get("method").stringValue());
            request.put("pathTemplate", template);
            ArrayNode values = request.putArray("pathParameters");
            for (int i = 1; i <= m.groupCount(); i++) values.add(decodePath(m.group(i)));
            ArrayNode query = request.putArray("query");
            if (received.rawQuery() != null && !received.rawQuery().isEmpty()) {
                for (String pair : received.rawQuery().split("&", -1)) {
                    int eq = pair.indexOf('=');
                    String name = eq < 0 ? pair : pair.substring(0, eq);
                    String value = eq < 0 ? "" : pair.substring(eq + 1);
                    query.addObject().put("name", decodeQuery(name)).put("value", decodeQuery(value));
                }
            }
            ArrayNode headers = request.putArray("headers");
            for (String name : headerParameters(operation)) {
                String value = received.header(name);
                if (value != null) headers.addObject().put("name", name).put("value", header(value));
            }
            String contentType = received.header("content-type");
            String mediaType = contentType == null ? null
                    : contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
            if (mediaType == null || received.body().length == 0) {
                request.putNull("contentType");
                request.putNull("body");
                return new Parsed(operation, request, null);
            }
            request.put("contentType", mediaType);
            try {
                request.set("body", Oracle.JSON.readTree(new String(received.body(), StandardCharsets.UTF_8)));
            } catch (RuntimeException e) {
                return new Parsed(operation, null, mediaType);
            }
            return new Parsed(operation, request, mediaType);
        }
        return null;
    }

    /** The names of an operation's header parameters, as the contract declares them. */
    private List<String> headerParameters(JsonNode operation) {
        String at = operation.get("location").stringValue();
        String item = at.substring(0, at.lastIndexOf('/'));
        List<String> out = new ArrayList<>();
        for (String parameters : List.of(item + "/parameters", at + "/parameters")) {
            JsonNode list = suite.oracle.document.at(parameters);
            for (int i = 0; i < list.size(); i++) {
                JsonNode p = suite.oracle.resolve(parameters + "/" + i);
                if (p.path("in").stringValue("").equals("header")) out.add(p.get("name").stringValue());
            }
        }
        return out;
    }

    private static Pattern pattern(String template) {
        StringBuilder out = new StringBuilder();
        Matcher m = PLACEHOLDER.matcher(template);
        int last = 0;
        while (m.find()) {
            out.append(Pattern.quote(template.substring(last, m.start()))).append("([^/]+)");
            last = m.end();
        }
        return Pattern.compile(out.append(Pattern.quote(template.substring(last))).toString());
    }

    /** A path segment decoded: percent-encoding only, a {@code +} kept. */
    static String decodePath(String raw) {
        return URLDecoder.decode(raw.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    /** A query name or value decoded, as a form: a {@code +} is a space. */
    static String decodeQuery(String raw) {
        return URLDecoder.decode(raw, StandardCharsets.UTF_8);
    }

    /**
     * A header value as a server reads it: as ISO 8859-1, the charset HTTP gives header octets,
     * which is how the JDK server delivers it and how the client sends it.
     */
    static String header(String value) {
        return value;
    }
}
