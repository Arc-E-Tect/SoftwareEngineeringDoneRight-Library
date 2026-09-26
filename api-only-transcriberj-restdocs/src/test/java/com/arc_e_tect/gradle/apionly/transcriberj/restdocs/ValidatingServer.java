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
 * A server that validates every request against the contract, as a real one would, with the
 * independent validator: parameters converted from the strings they travel as, bodies checked
 * strictly when strictness is on. Any violation is answered with the status and content type the
 * contract declares for an invalid request, and a body from the response class's generated
 * {@code requiredBody()}; a valid request with {@code 200}. Header values are read as ISO 8859-1,
 * as HTTP defines them.
 *
 * <p>It knows nothing of the cases: which case a request is for is never asked. Its variants,
 * for the tests that need a server to misbehave, change one thing: {@link #ignoring} stops
 * enforcing exactly one case's keyword at exactly its location, as an implementation that forgot
 * that one constraint would; {@link #answering} answers one case's violation with something
 * else.
 */
final class ValidatingServer implements ContractServer.Behaviour {

    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{[^}]+}");

    private final GeneratedSuite suite;
    private final RequestValidation validation;
    private final JsonNode target;
    private final Function<ContractServer.Received, ContractServer.Answer> targetAnswer;

    private ValidatingServer(GeneratedSuite suite, JsonNode target,
                             Function<ContractServer.Received, ContractServer.Answer> targetAnswer) {
        this.suite = suite;
        this.validation = new RequestValidation(suite.oracle, suite.settings.strictRequests());
        this.target = target;
        this.targetAnswer = targetAnswer;
    }

    /** The server that validates every request. */
    static ValidatingServer of(GeneratedSuite suite) {
        return new ValidatingServer(suite, null, null);
    }

    /** The server that does not enforce one case's keyword at its location, and accepts what only breaks that. */
    static ValidatingServer ignoring(GeneratedSuite suite, JsonNode c) {
        return new ValidatingServer(suite, c, r -> accepted());
    }

    /** The server that answers a request whose only violation is one case's with something else. */
    static ValidatingServer answering(GeneratedSuite suite, JsonNode c,
                                      Function<ContractServer.Received, ContractServer.Answer> answer) {
        return new ValidatingServer(suite, c, answer);
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
        List<RequestValidation.Problem> problems;
        if (parsed.request() == null) {
            problems = List.of(new RequestValidation.Problem("body", null, "json", "", null));
        } else {
            problems = validation.problems(parsed.request());
        }
        if (target != null && !problems.isEmpty()) {
            List<RequestValidation.Problem> rest = without(problems, target, parsed);
            if (rest.isEmpty()) return targetAnswer.apply(received);
        }
        return problems.isEmpty() ? accepted() : invalid(parsed.operation());
    }

    /** {@code 200}, with an empty JSON object. */
    static ContractServer.Answer accepted() {
        return new ContractServer.Answer(200, "application/json", "{}".getBytes(StandardCharsets.UTF_8));
    }

    /** The response the contract declares for an invalid request to an operation. */
    ContractServer.Answer invalid(JsonNode operation) {
        int status = Integer.parseInt(suite.settings.invalidRequestStatus());
        List<String> contentTypes = declaredContentTypes(operation);
        if (contentTypes.isEmpty()) return new ContractServer.Answer(status, null, null);
        return new ContractServer.Answer(status, contentTypes.get(0), requiredBody(operation));
    }

    /** The content types the operation's invalid-request response is declared with. */
    List<String> declaredContentTypes(JsonNode operation) {
        JsonNode response = suite.oracle.resolve(operation.get("location").stringValue() + "/responses/"
                + suite.settings.invalidRequestStatus());
        List<String> out = new ArrayList<>();
        response.path("content").properties().forEach(e -> out.add(e.getKey()));
        return out;
    }

    /** A valid body of the operation's invalid-request response: its class's {@code requiredBody()}. */
    byte[] requiredBody(JsonNode operation) {
        String bodyClass = operation.get("cases").get(0).get("responseBodyClass").stringValue(null);
        if (bodyClass == null) return "{}".getBytes(StandardCharsets.UTF_8);
        try {
            Object body = suite.type(suite.settings.basePackage() + "." + bodyClass).getMethod("requiredBody")
                    .invoke(null);
            return ((String) body).getBytes(StandardCharsets.UTF_8);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
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
        for (JsonNode operation : suite.report.get("invalidRequests")) {
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
