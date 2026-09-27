package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import com.arc_e_tect.gradle.apionly.transcriberj.model.MediaType;
import com.arc_e_tect.gradle.apionly.transcriberj.model.Operation;
import com.arc_e_tect.gradle.apionly.transcriberj.model.PathItem;
import com.arc_e_tect.gradle.apionly.transcriberj.model.Response;
import com.arc_e_tect.gradle.apionly.transcriberj.spi.ContractCase;
import com.arc_e_tect.gradle.apionly.transcriberj.spi.EmitterContext;
import com.arc_e_tect.gradle.apionly.transcriberj.spi.GeneratedClass;
import com.arc_e_tect.gradle.apionly.transcriberj.spi.ResponseBody;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * What the stubs of one contract are, before they are written as files or as Java: for each
 * operation, its cases and what its fallbacks match and answer. Both formats are written from it,
 * so that they describe the same stubs.
 *
 * @param contract        the contract's name
 * @param contractVersion its version
 * @param priority        the exact stubs' priority
 * @param fallbacks       whether the fallbacks are written
 * @param operations      every operation with a case or a fallback, in declaration order
 */
record Plan(String contract, String contractVersion, int priority, boolean fallbacks, List<Op> operations) {

    /** The fallbacks of one operation, in the order they are tried: each one's tier within the operation. */
    enum Fallback {

        /** An {@code Accept} no response satisfies. */
        NOT_ACCEPTABLE("not-acceptable", "NOT_ACCEPTABLE"),

        /** A body whose {@code Content-Type} the operation does not accept. */
        UNSUPPORTED_MEDIA_TYPE("unsupported-media-type", "UNSUPPORTED_MEDIA_TYPE"),

        /** A request as valid as the stub can tell. */
        VALID("valid", "SUCCESS"),

        /** Any other request to the operation. */
        INVALID("invalid", "INVALID_REQUEST");

        /** How the fallback's file and mapping are named, after {@code fallback-}. */
        final String id;

        /** The kind of case it answers as. */
        final String kind;

        Fallback(String id, String kind) {
            this.id = id;
            this.kind = kind;
        }

        /** The fallback's id, as its mapping's name and file name end: {@code fallback-<name>}. */
        String caseId() {
            return "fallback-" + id;
        }
    }

    /** The headers a request parameter cannot stand for: the core leaves them out, and so do the stubs. */
    private static final Set<String> IGNORED_HEADERS = Set.of("accept", "content-type", "authorization");

    /** How many priorities one operation's fallbacks take: one per {@link Fallback}. */
    static final int TIERS = Fallback.values().length;

    /**
     * A response, as a stub answers with it.
     *
     * @param status      its status
     * @param contentType its first content type, or {@code null} when it has no content
     * @param body        its body, or {@code null} when it has none the core generates
     */
    record Answer(int status, String contentType, ResponseBody body) {

        /** The body's class, or {@code null}. */
        String bodyClass() {
            return body == null ? null : body.bodyClass();
        }
    }

    /**
     * One operation.
     *
     * @param location      its JSON pointer
     * @param name          what its stubs are named with: its {@code operationId}, or its class stem
     * @param casesClass    the simple name of its {@code <Operation>ContractCases}
     * @param method        its method
     * @param pathTemplate  its path template
     * @param rank          how specific its path template is among its method's: {@code 0} for
     *                      the most specific
     * @param acceptable    what an acceptable {@code Accept} matches, or {@code null} for any
     * @param supported     what a supported {@code Content-Type} matches, or {@code null} for any
     * @param parameters    its path, query and header parameters, in the order the core lists them
     * @param bodyMediaType the first media type its request body is declared with, or {@code null}
     * @param bodySchema    that body's schema, as the core gives it, or {@code null}
     * @param bodyRequired  whether a request must have a body
     * @param uncheckedFormats the formats that body's schema asserts which the fallback does not
     *                      check by shape
     * @param answers       what each fallback it has answers with
     * @param cases         its contract cases
     */
    record Op(String location, String name, String casesClass, String method, String pathTemplate, int rank,
              String acceptable, String supported, List<Patterns.Parameter> parameters, String bodyMediaType,
              String bodySchema, boolean bodyRequired, List<String> uncheckedFormats, Map<Fallback, Answer> answers,
              List<ContractCase> cases) {

        /**
         * The priority of one of its fallbacks: below every exact stub, the operations of more
         * specific path templates first, and within one operation in the order they are tried.
         */
        int priority(int exact, Fallback fallback) {
            return exact + 1 + TIERS * rank + fallback.ordinal();
        }

        /** What its valid fallback enforces. */
        List<String> enforces() {
            List<String> out = new ArrayList<>(List.of("method and path template"));
            if (acceptable != null) out.add("Accept: absent, or one a declared response satisfies");
            for (Patterns.Parameter p : parameters) {
                if (p.enforces() != null) out.add(p.enforces());
            }
            if (bodyMediaType != null) {
                out.add("Content-Type: " + bodyMediaType + (bodyRequired ? "" : ", when there is a body"));
            }
            if (bodySchema != null) {
                out.add("body: its schema, a format of uuid, date, date-time or email by its shape");
            }
            return out;
        }

        /** What its valid fallback does not enforce. */
        List<String> doesNotEnforce() {
            List<String> out = new ArrayList<>();
            for (Patterns.Parameter p : parameters) {
                if (p.doesNotEnforce() != null) out.add(p.doesNotEnforce());
            }
            if (parameters.stream().anyMatch(p -> p.in().equals("path") && p.pattern() != null)) {
                out.add("path: a value that travels percent-encoded");
            }
            if (!uncheckedFormats.isEmpty()) out.add("body: format " + String.join(", ", uncheckedFormats));
            return out;
        }
    }

    /**
     * The plan of a contract.
     *
     * @param context the contract, and what the core derived from it
     * @param options the emitter's options
     * @return the plan
     */
    static Plan of(EmitterContext context, Options options) {
        List<Op> operations = new ArrayList<>();
        for (PathItem item : context.model().paths()) {
            for (Operation operation : item.operations()) {
                Op op = op(context, item, operation, options.fallbacks());
                if (op != null) operations.add(op);
            }
        }
        return new Plan(context.settings().contract(), context.model().version(), options.priority(),
                options.fallbacks(), ranked(operations));
    }

    private static Op op(EmitterContext context, PathItem item, Operation operation, boolean fallbacks) {
        String location = location(operation);
        List<ContractCase> cases = context.contractCases(location);
        String casesClass = context.names().contractCases(location).map(GeneratedClass::simpleName).orElse(null);
        if (casesClass == null) return null;
        String name = operation.operationId() != null ? operation.operationId()
                : casesClass.substring(0, casesClass.length() - "ContractCases".length());

        List<String> responseTypes = new ArrayList<>();
        if (operation.responses() != null) {
            for (Response r : operation.responses()) {
                if (r.content() != null) r.content().forEach(m -> responseTypes.add(m.contentType()));
            }
        }
        String acceptable = Patterns.acceptable(responseTypes.stream().distinct().toList());
        List<String> requestTypes = operation.requestBody() == null || operation.requestBody().content() == null
                ? List.of() : operation.requestBody().content().stream().map(MediaType::contentType).toList();
        String supported = Patterns.supported(requestTypes);
        String bodyMediaType = requestTypes.isEmpty() ? null : requestTypes.get(0);
        java.util.Set<String> unchecked = new java.util.TreeSet<>();
        String bodySchema = bodyMediaType == null ? null : context.requestBodySchema(location, bodyMediaType)
                .map(schema -> Patterns.shaped(schema, unchecked)).orElse(null);
        boolean bodyRequired = operation.requestBody() != null
                && Boolean.TRUE.equals(operation.requestBody().required());

        List<Patterns.Parameter> parameters = new ArrayList<>();
        for (com.arc_e_tect.gradle.apionly.transcriberj.model.Parameter p : parameters(item, operation)) {
            parameters.add(Patterns.parameter(p.in(), p.name(), "path".equals(p.in()) || Boolean.TRUE.equals(p.required()),
                    context.parameterSchema(location, p.in(), p.name()).orElse(null)));
        }

        Map<Fallback, Answer> answers = new TreeMap<>();
        if (fallbacks) {
            if (acceptable != null) answer(context, location, operation, "406").ifPresent(a -> answers.put(Fallback.NOT_ACCEPTABLE, a));
            if (supported != null) {
                answer(context, location, operation, "415").ifPresent(a -> answers.put(Fallback.UNSUPPORTED_MEDIA_TYPE, a));
            }
            firstSuccess(operation).flatMap(s -> answer(context, location, operation, s))
                    .ifPresent(a -> answers.put(Fallback.VALID, a));
            answer(context, location, operation, context.settings().invalidRequestStatus())
                    .ifPresent(a -> answers.put(Fallback.INVALID, a));
        }
        if (cases.isEmpty() && answers.isEmpty()) return null;
        return new Op(location, name, casesClass, operation.method().key().toUpperCase(Locale.ROOT), operation.path(),
                0, acceptable, supported, List.copyOf(parameters), bodyMediaType, bodySchema, bodyRequired,
                List.copyOf(unchecked), answers, cases);
    }

    /** The response an operation declares for a status, as a stub answers with it. */
    private static java.util.Optional<Answer> answer(EmitterContext context, String location, Operation operation,
                                                     String status) {
        if (operation.responses() == null) return java.util.Optional.empty();
        return operation.responses().stream().filter(r -> r.status().equals(status)).findFirst().map(r -> new Answer(
                Integer.parseInt(status),
                r.content() == null || r.content().isEmpty() ? null : r.content().get(0).contentType(),
                r.content() == null || r.content().isEmpty() ? null
                        : context.responseBody(location, status).orElse(null)));
    }

    /** The first {@code 2xx} an operation declares, written as a status. */
    private static java.util.Optional<String> firstSuccess(Operation operation) {
        if (operation.responses() == null) return java.util.Optional.empty();
        return operation.responses().stream().map(Response::status).filter(s -> s.matches("2[0-9][0-9]")).findFirst();
    }

    /**
     * The operation's parameters, as the core lists them: its path item's, each replaced where the
     * operation declares one of the same name and location, then its own; cookies, and the headers
     * no parameter can stand for, left out.
     */
    private static List<com.arc_e_tect.gradle.apionly.transcriberj.model.Parameter> parameters(PathItem item,
                                                                                            Operation operation) {
        List<com.arc_e_tect.gradle.apionly.transcriberj.model.Parameter> own = new ArrayList<>(
                operation.parameters() == null ? List.of() : operation.parameters());
        List<com.arc_e_tect.gradle.apionly.transcriberj.model.Parameter> out = new ArrayList<>();
        if (item.parameters() != null) {
            for (com.arc_e_tect.gradle.apionly.transcriberj.model.Parameter p : item.parameters()) {
                var override = own.stream().filter(o -> same(o, p)).findFirst();
                override.ifPresent(own::remove);
                out.add(override.orElse(p));
            }
        }
        out.addAll(own);
        out.removeIf(p -> p.name() == null || p.in() == null || "cookie".equals(p.in())
                || ("header".equals(p.in()) && IGNORED_HEADERS.contains(p.name().toLowerCase(Locale.ROOT))));
        return out;
    }

    private static boolean same(com.arc_e_tect.gradle.apionly.transcriberj.model.Parameter a,
                                com.arc_e_tect.gradle.apionly.transcriberj.model.Parameter b) {
        return a.name() != null && a.name().equals(b.name()) && a.in() != null && a.in().equals(b.in());
    }

    /**
     * The operations with each one's rank: among the path templates of one method, those whose
     * first differing segment is a literal where the other's is a placeholder come first, so that
     * {@code /users/me} is tried before {@code /users/{id}}.
     */
    private static List<Op> ranked(List<Op> operations) {
        Map<String, TreeSet<String>> templates = new TreeMap<>();
        for (Op op : operations) {
            templates.computeIfAbsent(op.method(), m -> new TreeSet<>(Plan::specificity)).add(op.pathTemplate());
        }
        List<Op> out = new ArrayList<>();
        for (Op op : operations) {
            int rank = new ArrayList<>(templates.get(op.method())).indexOf(op.pathTemplate());
            out.add(new Op(op.location(), op.name(), op.casesClass(), op.method(), op.pathTemplate(), rank,
                    op.acceptable(), op.supported(), op.parameters(), op.bodyMediaType(), op.bodySchema(),
                    op.bodyRequired(), op.uncheckedFormats(), op.answers(), op.cases()));
        }
        return List.copyOf(out);
    }

    /** More specific first: segment by segment, a literal before a placeholder; then fewer segments; then text. */
    static int specificity(String a, String b) {
        String[] x = a.split("/", -1);
        String[] y = b.split("/", -1);
        for (int i = 0; i < Math.min(x.length, y.length); i++) {
            int c = Boolean.compare(x[i].contains("{"), y[i].contains("{"));
            if (c != 0) return c;
        }
        return Comparator.<String>comparingInt(t -> t.split("/", -1).length).thenComparing(Comparator.naturalOrder())
                .compare(a, b);
    }

    /** The operation's JSON pointer, as the core keys it. */
    static String location(Operation operation) {
        return "/paths/" + operation.path().replace("~", "~0").replace("/", "~1") + "/" + operation.method().key();
    }

    /** Every body class the plan's stubs answer with, sorted. */
    Set<String> bodyClasses() {
        Set<String> out = new TreeSet<>();
        for (Op op : operations) {
            for (ContractCase c : op.cases()) {
                if (c.responseBodyClass() != null && !c.expectedContentTypes().isEmpty()) out.add(c.responseBodyClass());
            }
            op.answers().values().stream().map(Answer::bodyClass).filter(java.util.Objects::nonNull).forEach(out::add);
        }
        return out;
    }

    /** What makes a name safe as a file name: every character but letters, digits, {@code -}, {@code _} and {@code .} replaced. */
    static String fileName(String name) {
        String safe = name.replaceAll("[^A-Za-z0-9._-]", "_");
        return safe.matches("\\.*") ? "_" + safe : safe;
    }
}
