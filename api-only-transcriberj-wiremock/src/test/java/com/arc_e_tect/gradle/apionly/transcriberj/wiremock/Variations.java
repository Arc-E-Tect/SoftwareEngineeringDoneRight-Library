package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Valid requests no case sends, built from the cases' own and judged by the independent oracle
 * before they are sent: what a consumer sends that nobody generated.
 */
final class Variations {

    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    /**
     * One request.
     *
     * @param operation the operation's entry in the report
     * @param how       how it was made
     * @param request   the request
     */
    record Variation(JsonNode operation, String how, Replay.Request request) {

        @Override
        public String toString() {
            return operation.get("location").stringValue() + ": " + how;
        }
    }

    private Variations() {
    }

    /** A server holding only a suite's fallbacks, reading its bodies. */
    static DoubleServer fallbacksOnly(GeneratedSuite suite) {
        DoubleServer server = DoubleServer.bodiesOf(suite.files());
        for (StubMapping mapping : suite.stubMappings().values()) {
            if (mapping.getName().contains("/fallback-")) server.stub(mapping);
        }
        return server;
    }

    /**
     * Valid requests of every operation with success cases: the full body with each optional member
     * removed in turn, every other {@code enum} value of a body member or a parameter, another valid
     * path value, and an optional query parameter added -- each one the oracle finds valid.
     */
    static List<Variation> valid(GeneratedSuite suite) {
        List<Variation> out = new ArrayList<>();
        for (JsonNode operation : suite.report.get("contractCases")) {
            List<GeneratedSuite.Case> cases = suite.cases.stream().filter(c -> c.operation() == operation).toList();
            GeneratedSuite.Case required = first(cases, "SUCCESS", "required");
            GeneratedSuite.Case full = first(cases, "SUCCESS", "full");
            if (required == null) continue;
            Replay.Request base = suite.request(required);
            String location = operation.get("location").stringValue();
            if (full != null) {
                Replay.Request whole = suite.request(full);
                if (whole.body() != null && Oracle.JSON.readTree(whole.body()) instanceof ObjectNode body) {
                    JsonNode schema = suite.oracle.resolve(bodyPointer(location, whole.contentType()));
                    List<String> requiredMembers = new ArrayList<>();
                    schema.path("required").forEach(n -> requiredMembers.add(n.stringValue()));
                    for (String member : body.propertyNames()) {
                        if (requiredMembers.contains(member)) continue;
                        ObjectNode without = body.deepCopy();
                        without.remove(member);
                        add(out, suite, operation, "without optional member " + member, body(whole, without));
                    }
                    for (String member : body.propertyNames()) {
                        JsonNode property = suite.oracle.resolve(bodyPointer(location, whole.contentType())
                                + "/properties/" + escape(member));
                        for (JsonNode value : enumValues(suite, property)) {
                            if (value.equals(body.get(member))) continue;
                            ObjectNode changed = body.deepCopy();
                            changed.set(member, value);
                            add(out, suite, operation, member + " = " + value, body(whole, changed));
                        }
                    }
                }
                for (String[] parameter : whole.query()) {
                    if (base.query().stream().anyMatch(p -> p[0].equals(parameter[0]))) continue;
                    List<String[]> query = new ArrayList<>(base.query());
                    query.add(parameter);
                    add(out, suite, operation, "optional query " + parameter[0] + " added", new Replay.Request(
                            base.method(), base.pathTemplate(), base.pathParameters(), query, base.headers(),
                            base.contentType(), base.body()));
                }
            }
            GeneratedSuite.Case notFound = first(cases, "NOT_FOUND", null);
            if (notFound != null) {
                Replay.Request other = suite.request(notFound);
                add(out, suite, operation, "another path value " + other.pathParameters(), new Replay.Request(
                        base.method(), base.pathTemplate(), other.pathParameters(), base.query(), base.headers(),
                        base.contentType(), base.body()));
            }
            for (int i = 0; i < base.query().size(); i++) {
                String[] parameter = base.query().get(i);
                JsonNode schema = parameterSchema(suite, location, "query", parameter[0]);
                for (JsonNode value : enumValues(suite, schema)) {
                    if (value.asString().equals(parameter[1])) continue;
                    List<String[]> query = new ArrayList<>(base.query());
                    query.set(i, new String[]{parameter[0], value.asString()});
                    add(out, suite, operation, "query " + parameter[0] + " = " + value, new Replay.Request(
                            base.method(), base.pathTemplate(), base.pathParameters(), query, base.headers(),
                            base.contentType(), base.body()));
                }
            }
            for (String[] header : base.headers()) {
                JsonNode schema = parameterSchema(suite, location, "header", header[0]);
                for (JsonNode value : enumValues(suite, schema)) {
                    if (value.asString().equals(header[1])) continue;
                    add(out, suite, operation, "header " + header[0] + " = " + value,
                            Replay.withHeader(base, header[0], value.asString()));
                }
            }
        }
        return out;
    }

    private static GeneratedSuite.Case first(List<GeneratedSuite.Case> cases, String kind, String variant) {
        return cases.stream().filter(c -> c.kind().equals(kind)
                && (variant == null || variant.equals(c.field("variant")))).findFirst().orElse(null);
    }

    private static Replay.Request body(Replay.Request request, JsonNode body) {
        return new Replay.Request(request.method(), request.pathTemplate(), request.pathParameters(), request.query(),
                request.headers(), request.contentType(), Oracle.JSON.writeValueAsString(body) + "\n");
    }

    /** Adds a request when the oracle finds its body and every parameter valid. */
    private static void add(List<Variation> out, GeneratedSuite suite, JsonNode operation, String how,
                            Replay.Request request) {
        String location = operation.get("location").stringValue();
        if (request.body() != null) {
            JsonNode body = Oracle.JSON.readTree(request.body());
            if (!suite.oracle.valid(bodyPointer(location, request.contentType()), body)) return;
        }
        List<String> names = new ArrayList<>();
        String template = request.pathTemplate();
        for (int start = template.indexOf('{'); start >= 0; start = template.indexOf('{', start + 1)) {
            names.add(template.substring(start + 1, template.indexOf('}', start)));
        }
        for (int i = 0; i < names.size(); i++) {
            if (!valid(suite, location, "path", names.get(i), request.pathParameters().get(i))) return;
        }
        for (String[] parameter : request.query()) {
            if (!valid(suite, location, "query", parameter[0], parameter[1])) return;
        }
        out.add(new Variation(operation, how, request));
    }

    private static boolean valid(GeneratedSuite suite, String location, String in, String name, String value) {
        JsonNode schema = parameterSchema(suite, location, in, name);
        String pointer = parameterPointer(suite, location, in, name);
        if (schema.isMissingNode() || pointer == null) return true;
        return suite.oracle.valid(pointer, typed(schema, value));
    }

    /** A parameter's value as the JSON value its schema describes. */
    static JsonNode typed(JsonNode schema, String value) {
        String type = schema.path("type").isString() ? schema.path("type").stringValue() : "";
        try {
            return switch (type) {
                case "integer", "number" -> NODES.numberNode(new java.math.BigDecimal(value));
                case "boolean" -> value.equals("true") || value.equals("false")
                        ? NODES.booleanNode(Boolean.parseBoolean(value)) : NODES.stringNode(value);
                default -> NODES.stringNode(value);
            };
        } catch (NumberFormatException e) {
            return NODES.stringNode(value);
        }
    }

    /** A schema's {@code enum} values, or none. */
    private static List<JsonNode> enumValues(GeneratedSuite suite, JsonNode schema) {
        List<JsonNode> out = new ArrayList<>();
        schema.path("enum").forEach(out::add);
        return out;
    }

    /** The pointer of an operation's request body schema, in a media type. */
    static String bodyPointer(String location, String mediaType) {
        return location + "/requestBody/content/" + escape(mediaType) + "/schema";
    }

    /** The resolved schema of a parameter, or a missing node. */
    static JsonNode parameterSchema(GeneratedSuite suite, String location, String in, String name) {
        String pointer = parameterPointer(suite, location, in, name);
        return pointer == null ? tools.jackson.databind.node.MissingNode.getInstance() : suite.oracle.resolve(pointer);
    }

    /** The pointer of a parameter's schema: the operation's own, or its path item's. */
    static String parameterPointer(GeneratedSuite suite, String location, String in, String name) {
        String item = location.substring(0, location.lastIndexOf('/'));
        for (String owner : List.of(location, item)) {
            JsonNode parameters = suite.oracle.document.at(owner + "/parameters");
            for (int i = 0; i < parameters.size(); i++) {
                String at = owner + "/parameters/" + i;
                JsonNode parameter = suite.oracle.resolve(at);
                if (name.equals(parameter.path("name").stringValue()) && in.equals(parameter.path("in").stringValue())) {
                    String ref = suite.oracle.document.at(at).path("$ref").stringValue(null);
                    return (ref != null ? ref.substring(1) : at) + "/schema";
                }
            }
        }
        return null;
    }

    static String escape(String segment) {
        return segment.replace("~", "~0").replace("/", "~1");
    }
}
