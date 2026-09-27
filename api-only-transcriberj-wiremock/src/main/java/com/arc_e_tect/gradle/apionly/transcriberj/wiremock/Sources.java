package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import java.util.ArrayList;
import java.util.List;

/** The source of the class the {@code java} format generates. */
final class Sources {

    /** The generated class's simple name. */
    static final String STUBS = "ContractStubs";

    private static final String INDENT = "    ";

    /** Longer string constants are split: a class-file constant holds at most 65535 bytes. */
    private static final int CHUNK = 8000;

    private Sources() {
    }

    /**
     * {@code ContractStubs}: the same stubs the {@code files} format writes, as Java building
     * WireMock mappings, generic over the cases and over a table of the operations.
     *
     * @param header the comment every generated file starts with
     * @param pkg    the package it goes in
     * @param base   the base package, where the core's classes are
     * @param plan   the stubs
     * @return the source
     */
    static String stubs(String header, String pkg, String base, Plan plan) {
        String i2 = INDENT + INDENT;
        StringBuilder operations = new StringBuilder();
        for (int i = 0; i < plan.operations().size(); i++) {
            operations.append(operation(plan.operations().get(i)));
            operations.append(i + 1 < plan.operations().size() ? ",\n" : "");
        }
        List<String> casesClasses = plan.operations().stream().filter(op -> !op.cases().isEmpty())
                .map(op -> base + "." + op.casesClass() + ".CASES").toList();
        String cases = casesClasses.isEmpty() ? "List.of()" : "List.of(\n" + i2 + INDENT
                + String.join(",\n" + i2 + INDENT, casesClasses) + ")";
        StringBuilder bodies = new StringBuilder();
        if (plan.bodyClasses().isEmpty()) {
            bodies.append(i2).append(unknown(i2));
        } else {
            bodies.append(i2).append("return switch (bodyClass) {\n");
            for (String bodyClass : plan.bodyClasses()) {
                bodies.append(i2).append(INDENT).append("case \"").append(bodyClass).append("\" -> ").append(base)
                        .append('.').append(bodyClass).append(".fullBody();\n");
            }
            bodies.append(i2).append(INDENT).append("default -> ").append(unknown(i2 + INDENT)).append(i2).append("};\n");
        }
        return header + TEMPLATE.formatted(pkg, base, plan.priority(), literal(plan.contract()),
                literal(plan.contractVersion()), operations, cases, bodies, Plan.TIERS);
    }

    /** One operation of the table, as the constructor call that makes it. */
    private static String operation(Plan.Op op) {
        String i3 = INDENT + INDENT + INDENT;
        List<String> args = new ArrayList<>();
        args.add(literal(op.name()));
        args.add(literal(op.method()));
        args.add(literal(op.pathTemplate()));
        args.add(String.valueOf(op.rank()));
        args.add(literal(op.acceptable()));
        args.add(literal(op.supported()));
        List<String> parameters = op.parameters().stream().map(p -> "new Parameter(" + literal(p.in()) + ", "
                + literal(p.name()) + ", " + p.required() + ", " + literal(p.pattern()) + ")").toList();
        args.add(list(parameters, i3 + INDENT));
        args.add(literal(op.bodyMediaType()));
        args.add(literal(op.bodySchema()));
        args.add(String.valueOf(op.bodyRequired()));
        for (Plan.Fallback fallback : Plan.Fallback.values()) args.add(answer(op.answers().get(fallback)));
        args.add(list(op.enforces().stream().map(Sources::literal).toList(), i3 + INDENT));
        args.add(list(op.doesNotEnforce().stream().map(Sources::literal).toList(), i3 + INDENT));
        return INDENT + INDENT + "new Operation(\n" + i3 + String.join(",\n" + i3, args) + ")";
    }

    private static String answer(Plan.Answer answer) {
        return answer == null ? "null" : "new Answer(" + answer.status() + ", " + literal(answer.contentType()) + ", "
                + literal(answer.bodyClass()) + ")";
    }

    private static String list(List<String> items, String indent) {
        if (items.isEmpty()) return "List.of()";
        return "List.of(\n" + indent + String.join(",\n" + indent, items) + ")";
    }

    /** A text as a Java expression: a literal, or literals joined where one would be too long; or {@code null}. */
    static String literal(String text) {
        if (text == null) return "null";
        if (text.length() <= CHUNK) return quoted(text);
        List<String> parts = new ArrayList<>();
        for (int start = 0; start < text.length(); ) {
            int end = Math.min(start + CHUNK, text.length());
            if (end < text.length() && Character.isHighSurrogate(text.charAt(end - 1))) end--;
            parts.add(quoted(text.substring(start, end)));
            start = end;
        }
        return "String.join(\"\", " + String.join(", ", parts) + ")";
    }

    private static String quoted(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20 || c > 0x7e) {
                        out.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    /** The statement for a body class no stub answers with, its continuation indented from where it starts. */
    private static String unknown(String indent) {
        return "throw new IllegalArgumentException(\"No response body class \" + bodyClass\n"
                + indent + INDENT + INDENT + "+ \" in this contract's stubs.\");\n";
    }

    /**
     * The class, with its parameters: 1 the package, 2 the base package, 3 the priority, 4 the
     * contract, 5 its version, 6 the operations, 7 the cases, 8 the body switch, 9 the tiers.
     */
    private static final String TEMPLATE = """
            package %1$s;

            import %2$s.CaseKind;
            import %2$s.ContractCase;
            import %2$s.ContractRequest;
            import com.github.tomakehurst.wiremock.client.MappingBuilder;
            import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
            import com.github.tomakehurst.wiremock.client.WireMock;
            import com.github.tomakehurst.wiremock.matching.StringValuePattern;

            import java.nio.charset.StandardCharsets;
            import java.util.ArrayList;
            import java.util.LinkedHashMap;
            import java.util.List;
            import java.util.Map;
            import java.util.regex.Matcher;
            import java.util.regex.Pattern;

            /**
             * WireMock stubs for the contract: an early draft of the API that answers as the contract says, the
             * same stubs the WireMock emitter's {@code files} format writes as mapping files.
             *
             * <ul>
             *   <li>{@link #mappingFor(ContractCase)}: a case's exact stub, matching exactly its request.</li>
             *   <li>{@link #fallbacks()}: for every operation, the stubs that answer the requests no case sends,
             *       as closely to the contract as WireMock's matchers allow.</li>
             *   <li>{@link #all()}: every case's stub and every fallback.</li>
             * </ul>
             *
             * <p>Register them on whichever WireMock the tests reach -- {@code server.stubFor(mapping)},
             * {@code WireMock.stubFor(mapping)}, or {@code wireMock.register(mapping)} -- in a contract test's hooks:
             *
             * <pre>{@code
             * public void arrangeState(ContractCase contractCase) {
             *     wireMockServer.stubFor(ContractStubs.mappingFor(contractCase));
             * }
             * }</pre>
             *
             * <p><b>Generated, and meant to be copied.</b> This file is overwritten on every build. To change a stub,
             * copy this class into a source set of your own, in a package of your own: it uses only the public
             * classes of the generated tree, and keeps compiling against it. Change first what you came to change:
             * a response body in {@code body(String)} -- replace a {@code fullBody()} with a literal -- or an
             * operation's row in {@link #OPERATIONS}, or a matcher in {@code mappingFor} or {@code valid}.
             */
            @com.arc_e_tect.sedr.utils.jacoco.marker.ExcludeFromJacocoGeneratedCodeCoverage(justification = "Generated by API-Only TranscriberJ")
            public final class ContractStubs {

                /** The priority of every exact stub; the fallbacks come after it. */
                public static final int PRIORITY = %3$d;

                /** The contract the stubs are generated from. */
                public static final String CONTRACT = %4$s;

                /** Its version. */
                public static final String CONTRACT_VERSION = %5$s;

                /** How many priorities one operation's fallbacks take: one for each. */
                private static final int TIERS = %9$d;

                private static final String UNRESERVED =
                        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";
                private static final Pattern PLACEHOLDER = Pattern.compile("\\\\{[^}]+}");
                private static final String SPECIAL = "\\\\.[]{}()*+?^$|";

                /**
                 * One operation: what its stubs match and answer beyond each case's own request.
                 *
                 * @param name                 what its stubs are named with: its operationId, or its class stem
                 * @param method               its method
                 * @param pathTemplate         its path template
                 * @param rank                 how specific its path template is among its method's; 0 is the most
                 * @param acceptable           what an acceptable {@code Accept} matches, or null for any
                 * @param supported            what a supported {@code Content-Type} matches, or null for any
                 * @param parameters           its path, query and header parameters
                 * @param bodyMediaType        the first media type of its request body, or null
                 * @param bodySchema           that body's JSON Schema 2020-12, or null
                 * @param bodyRequired         whether a request must have a body
                 * @param notAcceptable        what its not-acceptable fallback answers, or null for none
                 * @param unsupportedMediaType what its unsupported-media-type fallback answers, or null for none
                 * @param valid                what its valid fallback answers, or null for none
                 * @param invalid              what its invalid fallback answers, or null for none
                 * @param enforces             what its valid fallback enforces
                 * @param doesNotEnforce       what its valid fallback does not enforce
                 */
                public record Operation(String name, String method, String pathTemplate, int rank, String acceptable,
                                        String supported, List<Parameter> parameters, String bodyMediaType,
                                        String bodySchema, boolean bodyRequired, Answer notAcceptable,
                                        Answer unsupportedMediaType, Answer valid, Answer invalid,
                                        List<String> enforces, List<String> doesNotEnforce) {
                }

                /**
                 * A parameter, as the valid fallback matches it.
                 *
                 * @param in       path, query or header
                 * @param name     its name
                 * @param required whether a request must send it
                 * @param pattern  what its value must match, or null when a regular expression checks nothing of it
                 */
                public record Parameter(String in, String name, boolean required, String pattern) {
                }

                /**
                 * A response, as a fallback answers with it.
                 *
                 * @param status      its status
                 * @param contentType its content type, or null when it has no content
                 * @param bodyClass   the class whose {@code fullBody()} it answers with, or null
                 */
                public record Answer(int status, String contentType, String bodyClass) {
                }

                /** Every operation with a case or a fallback, in declaration order. */
                public static final List<Operation> OPERATIONS = List.of(
            %6$s);

                private static final List<List<ContractCase>> CASES = %7$s;

                private ContractStubs() {
                }

                /**
                 * A case's exact stub: it matches exactly the case's request, and answers as the contract declares.
                 * Named {@code <operation>/<caseId>}, so that WireMock's near-miss reports and request journal say
                 * which case a stub belongs to.
                 *
                 * @param contractCase the case
                 * @return the mapping
                 */
                public static MappingBuilder mappingFor(ContractCase contractCase) {
                    ContractRequest request = contractCase.request();
                    Operation operation = operation(request.method(), request.pathTemplate());
                    MappingBuilder mapping = WireMock.request(request.method(), WireMock.urlPathEqualTo(path(request)))
                            .withName(operation.name() + "/" + contractCase.id())
                            .atPriority(PRIORITY)
                            .withMetadata(metadata(operation, contractCase.id(), contractCase.kind().name(), false));
                    Map<String, List<String>> query = new LinkedHashMap<>();
                    for (ContractRequest.Pair parameter : request.query()) {
                        query.computeIfAbsent(parameter.name(), name -> new ArrayList<>()).add(parameter.value());
                    }
                    for (Map.Entry<String, List<String>> parameter : query.entrySet()) {
                        mapping = mapping.withQueryParam(parameter.getKey(),
                                WireMock.havingExactly(parameter.getValue().toArray(String[]::new)));
                    }
                    for (String name : contractCase.declaredQuery()) {
                        if (!query.containsKey(name)) mapping = mapping.withQueryParam(name, WireMock.absent());
                    }
                    List<String> sent = new ArrayList<>();
                    for (ContractRequest.Pair header : request.headers()) {
                        sent.add(header.name());
                        if (!header.name().equalsIgnoreCase("Accept")) {
                            mapping = mapping.withHeader(header.name(), WireMock.equalTo(header.value()));
                        }
                    }
                    for (String name : contractCase.declaredHeaders()) {
                        if (sent.stream().noneMatch(name::equalsIgnoreCase)) {
                            mapping = mapping.withHeader(name, WireMock.absent());
                        }
                    }
                    if (operation.acceptable() != null) {
                        mapping = mapping.withHeader("Accept", contractCase.kind() == CaseKind.NOT_ACCEPTABLE
                                ? unacceptable(operation.acceptable()) : absentOr(operation.acceptable()));
                    }
                    if (request.body() == null) {
                        mapping = mapping.withRequestBody(WireMock.absent());
                    } else {
                        mapping = mapping
                                .withHeader("Content-Type", WireMock.matching(contentType(request.contentType())))
                                .withRequestBody(WireMock.equalToJson(request.body(), false, false));
                    }
                    List<String> types = contractCase.expectedContentTypes();
                    return mapping.willReturn(response(contractCase.expectedStatus(), types.isEmpty() ? null : types.get(0),
                            types.isEmpty() ? null : contractCase.responseBodyClass()));
                }

                /**
                 * Every operation's fallbacks: the stubs that answer the requests no case sends.
                 *
                 * @return the mappings
                 */
                public static List<MappingBuilder> fallbacks() {
                    List<MappingBuilder> out = new ArrayList<>();
                    for (Operation operation : OPERATIONS) out.addAll(fallbacks(operation));
                    return out;
                }

                /**
                 * One operation's fallbacks, in the order they are tried: not acceptable, unsupported media type,
                 * valid, invalid -- each where the operation has it. Each has a lower priority than every exact stub,
                 * and an operation whose path template is more specific has its fallbacks tried first.
                 *
                 * @param operation the operation
                 * @return the mappings
                 */
                public static List<MappingBuilder> fallbacks(Operation operation) {
                    List<MappingBuilder> out = new ArrayList<>();
                    int priority = PRIORITY + 1 + TIERS * operation.rank();
                    if (operation.notAcceptable() != null) {
                        out.add(fallback(operation, "not-acceptable", "NOT_ACCEPTABLE", priority, operation.notAcceptable())
                                .withHeader("Accept", unacceptable(operation.acceptable())));
                    }
                    if (operation.unsupportedMediaType() != null) {
                        out.add(fallback(operation, "unsupported-media-type", "UNSUPPORTED_MEDIA_TYPE", priority + 1,
                                operation.unsupportedMediaType())
                                .withHeader("Content-Type", unacceptable(operation.supported())));
                    }
                    if (operation.valid() != null) {
                        out.add(valid(fallback(operation, "valid", "SUCCESS", priority + 2, operation.valid()), operation));
                    }
                    if (operation.invalid() != null) {
                        out.add(fallback(operation, "invalid", "INVALID_REQUEST", priority + 3, operation.invalid()));
                    }
                    return out;
                }

                /**
                 * Every case's exact stub, and every fallback.
                 *
                 * @return the mappings
                 */
                public static List<MappingBuilder> all() {
                    List<MappingBuilder> out = new ArrayList<>();
                    for (List<ContractCase> cases : CASES) {
                        for (ContractCase contractCase : cases) out.add(mappingFor(contractCase));
                    }
                    out.addAll(fallbacks());
                    return out;
                }

                /** A fallback matching the operation's method and path template, and answering as given. */
                private static MappingBuilder fallback(Operation operation, String id, String kind, int priority,
                                                       Answer answer) {
                    return WireMock.request(operation.method(), WireMock.urlPathTemplate(operation.pathTemplate()))
                            .withName(operation.name() + "/fallback-" + id)
                            .atPriority(priority)
                            .withMetadata(metadata(operation, "fallback-" + id, kind, true))
                            .willReturn(response(answer.status(), answer.contentType(), answer.bodyClass()));
                }

                /** The valid fallback's matchers: each parameter by its pattern, the media types, and the body's schema. */
                private static MappingBuilder valid(MappingBuilder mapping, Operation operation) {
                    for (Parameter parameter : operation.parameters()) {
                        StringValuePattern pattern = parameter.pattern() == null
                                ? (parameter.required() ? WireMock.matching(".*") : null)
                                : parameter.required() || parameter.in().equals("path") ? WireMock.matching(parameter.pattern())
                                : absentOr(parameter.pattern());
                        if (pattern == null) continue;
                        mapping = switch (parameter.in()) {
                            case "path" -> mapping.withPathParam(parameter.name(), pattern);
                            case "query" -> mapping.withQueryParam(parameter.name(), pattern);
                            default -> mapping.withHeader(parameter.name(), pattern);
                        };
                    }
                    if (operation.acceptable() != null) mapping = mapping.withHeader("Accept", absentOr(operation.acceptable()));
                    if (operation.bodyMediaType() != null) {
                        String contentType = contentType(operation.bodyMediaType());
                        mapping = mapping.withHeader("Content-Type",
                                operation.bodyRequired() ? WireMock.matching(contentType) : absentOr(contentType));
                    }
                    if (operation.bodySchema() != null) {
                        StringValuePattern schema = WireMock.matchingJsonSchema(operation.bodySchema(),
                                WireMock.JsonSchemaVersion.V202012);
                        mapping = mapping.withRequestBody(operation.bodyRequired() ? schema : WireMock.or(WireMock.absent(), schema));
                    }
                    return mapping;
                }

                /** What the contract declares the response is. */
                private static ResponseDefinitionBuilder response(int status, String contentType, String bodyClass) {
                    ResponseDefinitionBuilder response = WireMock.aResponse().withStatus(status);
                    if (contentType == null) return response;
                    response = response.withHeader("Content-Type", contentType);
                    return bodyClass == null ? response : response.withBody(body(bodyClass));
                }

                /** The full body of a response body class, by the class's simple name. */
                private static String body(String bodyClass) {
            %8$s    }

                /** A stub's metadata: the contract, the operation, the case or fallback, its kind, and whether it is a fallback. */
                private static Map<String, Object> metadata(Operation operation, String caseId, String kind, boolean fallback) {
                    Map<String, Object> metadata = new LinkedHashMap<>();
                    metadata.put("contract", CONTRACT);
                    metadata.put("contractVersion", CONTRACT_VERSION);
                    metadata.put("operation", operation.name());
                    metadata.put("case", caseId);
                    metadata.put("kind", kind);
                    metadata.put("fallback", fallback);
                    if (caseId.equals("fallback-valid")) {
                        metadata.put("enforces", operation.enforces());
                        metadata.put("doesNotEnforce", operation.doesNotEnforce());
                    }
                    return metadata;
                }

                /** The operation a request belongs to. */
                private static Operation operation(String method, String pathTemplate) {
                    for (Operation operation : OPERATIONS) {
                        if (operation.method().equals(method) && operation.pathTemplate().equals(pathTemplate)) return operation;
                    }
                    throw new IllegalArgumentException("No operation " + method + " " + pathTemplate + " in contract "
                            + CONTRACT + ".");
                }

                private static StringValuePattern absentOr(String pattern) {
                    return WireMock.or(WireMock.absent(), WireMock.matching(pattern));
                }

                /** Present, and not matching: {@code not} alone would also match an absent header. */
                private static StringValuePattern unacceptable(String pattern) {
                    return WireMock.and(WireMock.matching(".*"), WireMock.not(WireMock.matching(pattern)));
                }

                /** A content type, in any case, with nothing after it but an optional charset. */
                private static String contentType(String contentType) {
                    StringBuilder out = new StringBuilder("(?i)");
                    for (int i = 0; i < contentType.length(); i++) {
                        char c = contentType.charAt(i);
                        if (SPECIAL.indexOf(c) >= 0) out.append('\\\\');
                        out.append(c);
                    }
                    return out.append("(\\\\s*;\\\\s*charset=[^;]+)?").toString();
                }

                /** The path as the request travels: the template filled with the values, each percent-encoded. */
                private static String path(ContractRequest request) {
                    Matcher placeholder = PLACEHOLDER.matcher(request.pathTemplate());
                    StringBuilder out = new StringBuilder();
                    int i = 0;
                    while (placeholder.find()) {
                        placeholder.appendReplacement(out,
                                Matcher.quoteReplacement(encode(request.pathParameters().get(i++))));
                    }
                    placeholder.appendTail(out);
                    return out.toString();
                }

                /** A value percent-encoded as UTF-8, except the characters RFC 3986 leaves unreserved. */
                private static String encode(String value) {
                    StringBuilder out = new StringBuilder();
                    for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
                        if (b >= 0 && UNRESERVED.indexOf(b) >= 0) {
                            out.append((char) b);
                        } else {
                            out.append('%%').append(String.format(java.util.Locale.ROOT, "%%02X", b & 0xff));
                        }
                    }
                    return out.toString();
                }
            }
            """;
}
