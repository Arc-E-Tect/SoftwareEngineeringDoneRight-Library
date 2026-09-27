package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The regular expressions the stubs match headers and parameters with: which {@code Accept} a
 * response satisfies, which {@code Content-Type} a request body may have, and which values a
 * parameter's schema allows, as closely as a regular expression can say it.
 */
final class Patterns {

    /** What any value but an absent one matches: WireMock's way of saying "present". */
    static final String PRESENT = ".*";

    /**
     * The parameter keywords a fallback enforces, for the values a regular expression can check.
     * The README lists them; {@code FallbackInvalidRequestTest} holds the two to each other.
     */
    static final List<String> ENFORCED = List.of("type", "enum", "const", "pattern", "minLength", "maxLength",
            "format");

    /** The formats a fallback checks, where the contract's {@code validateFormats} names them. */
    static final Map<String, String> FORMATS = Map.of(
            "uuid", "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
            "date", "[0-9]{4}-[0-9]{2}-[0-9]{2}",
            "date-time", "[0-9]{4}-[0-9]{2}-[0-9]{2}[Tt][0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]+)?([Zz]|[+-][0-9]{2}:[0-9]{2})",
            "email", "[^@\\s]+@[^@\\s]+");

    /** A value that travels percent-encoded: the one a fallback cannot check against its schema. */
    private static final String ENCODED = ".*%[0-9A-Fa-f]{2}.*";

    /** The keywords that constrain a value, whether or not a regular expression can check them. */
    private static final Set<String> CONSTRAINTS = Set.of("type", "enum", "const", "pattern", "minLength",
            "maxLength", "format", "minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum", "multipleOf",
            "items", "minItems", "maxItems", "uniqueItems", "contains", "properties", "required",
            "additionalProperties", "minProperties", "maxProperties", "allOf", "anyOf", "oneOf", "not");

    private static final String SPECIAL = "\\.[]{}()*+?^$|";

    private Patterns() {
    }

    /**
     * A text matched literally: every character a regular expression gives a meaning escaped.
     *
     * @param literal the text
     * @return the expression
     */
    static String escape(String literal) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < literal.length(); i++) {
            char c = literal.charAt(i);
            if (SPECIAL.indexOf(c) >= 0) out.append('\\');
            out.append(c);
        }
        return out.toString();
    }

    /**
     * The content type a case sends, in any case, with nothing after it but an optional
     * {@code charset} parameter.
     *
     * @param contentType the case's content type
     * @return the expression
     */
    static String contentType(String contentType) {
        return "(?i)" + escape(contentType) + "(\\s*;\\s*charset=[^;]+)?";
    }

    /**
     * An {@code Accept} some declared response satisfies: one whose list holds {@code *}{@code /*},
     * a range covering a declared media type, or a declared media type, each with any parameters.
     *
     * @param declared every media type the operation's responses are declared with
     * @return the expression, or {@code null} when every {@code Accept} is acceptable -- no
     *         response has content, or one is declared for any media type
     */
    static String acceptable(List<String> declared) {
        if (declared.isEmpty()) return null;
        Set<String> alternatives = new LinkedHashSet<>();
        alternatives.add("\\*/\\*");
        for (String mediaType : declared) {
            String[] essence = essence(mediaType);
            if (essence[0].equals("*")) return null;
            alternatives.add(escape(essence[0]) + "/\\*");
            alternatives.add(escape(essence[0]) + "/" + (essence[1].equals("*") ? "[^,;\\s]+" : escape(essence[1])));
        }
        return "(?i)(.*,)?\\s*(" + String.join("|", alternatives) + ")\\s*(;[^,]*)?(,.*)?";
    }

    /**
     * A {@code Content-Type} the request body is declared with, with any parameters.
     *
     * @param declared every media type the request body is declared with
     * @return the expression, or {@code null} when every content type is: none is declared, or
     *         one is declared for any media type
     */
    static String supported(List<String> declared) {
        if (declared.isEmpty()) return null;
        Set<String> alternatives = new LinkedHashSet<>();
        for (String mediaType : declared) {
            String[] essence = essence(mediaType);
            if (essence[0].equals("*")) return null;
            alternatives.add(escape(essence[0]) + "/" + (essence[1].equals("*") ? "[^;\\s]+" : escape(essence[1])));
        }
        return "(?i)\\s*(" + String.join("|", alternatives) + ")\\s*(;.*)?";
    }

    /** A media type's type and subtype, lower case, without parameters. */
    private static String[] essence(String mediaType) {
        String bare = mediaType.split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
        String[] parts = bare.split("/", 2);
        return new String[]{parts[0], parts.length > 1 ? parts[1] : "*"};
    }

    /**
     * What a fallback matches a parameter with, and what of its schema that checks.
     *
     * @param in            {@code path}, {@code query} or {@code header}
     * @param name          its name
     * @param required      whether a request must send it
     * @param pattern       the expression its value must match, or {@code null} when a regular
     *                      expression can check nothing of it
     * @param enforced      the schema's keywords the expression checks
     * @param notEnforced   the schema's keywords it does not
     */
    record Parameter(String in, String name, boolean required, String pattern, List<String> enforced,
                     List<String> notEnforced) {

        /**
         * What the fallback enforces of this parameter, for the stub's metadata.
         *
         * @return a sentence, or {@code null} when it enforces nothing
         */
        String enforces() {
            List<String> parts = new ArrayList<>();
            if (required) parts.add("present");
            parts.addAll(enforced);
            if (parts.isEmpty()) return null;
            return in + " " + name + ": " + String.join(", ", parts)
                    + (in.equals("path") && pattern != null ? " (unless percent-encoded)" : "");
        }

        /**
         * What the fallback does not enforce of this parameter, for the stub's metadata.
         *
         * @return a sentence, or {@code null} when it enforces everything
         */
        String doesNotEnforce() {
            return notEnforced.isEmpty() ? null : in + " " + name + ": " + String.join(", ", notEnforced);
        }
    }

    /**
     * A parameter as a fallback matches it.
     *
     * @param in       {@code path}, {@code query} or {@code header}
     * @param name     its name
     * @param required whether a request must send it
     * @param schema   its schema, as the core gives it; {@code null} when it has none
     * @return the parameter
     */
    static Parameter parameter(String in, String name, boolean required, String schema) {
        if (schema == null) return new Parameter(in, name, required, null, List.of(), List.of());
        @SuppressWarnings("unchecked")
        Map<String, Object> document = (Map<String, Object>) Json.read(schema);
        Map<String, Object> resolved = resolve(document, document);
        List<String> enforced = new ArrayList<>();
        List<String> notEnforced = new ArrayList<>();
        String value = value(resolved, enforced, notEnforced);
        String pattern = value == null ? null : in.equals("path") ? "(?s)(?:" + value + ")|" + ENCODED
                : "(?s)" + value;
        return new Parameter(in, name, required, pattern, List.copyOf(enforced), List.copyOf(notEnforced));
    }

    /**
     * A request body schema as the valid fallback checks it: WireMock's {@code matchesJsonSchema}
     * asserts no {@code format}, so beside each format the fallback checks by shape -- the
     * TranscriberJ keeps {@code format} only where {@code validateFormats} names it -- a
     * {@code pattern} of that shape, anchored.
     *
     * @param schema    the schema, as the core gives it
     * @param unchecked filled with every other format the schema asserts, which stays unchecked
     * @return the schema, with the patterns
     */
    static String shaped(String schema, java.util.Set<String> unchecked) {
        Object document = Json.read(schema);
        return shape(document, unchecked) ? Json.compact(document) : schema;
    }

    @SuppressWarnings("unchecked")
    private static boolean shape(Object node, java.util.Set<String> unchecked) {
        boolean changed = false;
        if (node instanceof Map<?, ?> map) {
            if (map.get("format") instanceof String format) {
                if (!FORMATS.containsKey(format)) {
                    unchecked.add(format);
                } else if (!map.containsKey("pattern")) {
                    ((Map<String, Object>) map).put("pattern", "^(?:" + FORMATS.get(format) + ")$");
                    changed = true;
                }
            }
            for (Object value : map.values()) changed |= shape(value, unchecked);
        } else if (node instanceof List<?> list) {
            for (Object value : list) changed |= shape(value, unchecked);
        }
        return changed;
    }

    /** A schema, its {@code $ref} into the document's {@code $defs} followed. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> resolve(Map<String, Object> schema, Map<String, Object> document) {
        Map<String, Object> current = schema;
        for (int i = 0; i < 16 && current.get("$ref") instanceof String ref && ref.startsWith("#/$defs/"); i++) {
            Object defs = document.get("$defs");
            if (!(defs instanceof Map<?, ?> map)
                    || !(map.get(ref.substring("#/$defs/".length())) instanceof Map<?, ?> target)) {
                break;
            }
            current = (Map<String, Object>) target;
        }
        return current;
    }

    /**
     * The expression a value of a schema matches, recording the keywords it checks and those it
     * cannot; {@code null} when it can check nothing.
     */
    private static String value(Map<String, Object> schema, List<String> enforced, List<String> notEnforced) {
        List<String> present = schema.keySet().stream().filter(CONSTRAINTS::contains).sorted().toList();
        String type = type(schema.get("type"));
        List<Object> literals = literals(schema);
        if (literals != null) {
            List<String> alternatives = literals.stream().filter(v -> v != null).map(Patterns::text)
                    .map(Patterns::escape).distinct().toList();
            for (String keyword : present) {
                (keyword.equals("enum") || keyword.equals("const") || keyword.equals("type") ? enforced : notEnforced)
                        .add(keyword);
            }
            return "(" + String.join("|", alternatives) + ")";
        }
        String pattern;
        List<String> checked = new ArrayList<>(List.of("type"));
        switch (type == null ? "" : type) {
            case "boolean" -> pattern = "(true|false)";
            case "integer" -> pattern = "-?[0-9]+";
            case "number" -> pattern = "-?[0-9]+(\\.[0-9]+)?([eE][+-]?[0-9]+)?";
            case "string" -> {
                StringBuilder out = new StringBuilder();
                if (schema.get("pattern") instanceof String p) {
                    out.append("(?=.*(?:").append(p).append("))");
                    checked.add("pattern");
                }
                Integer min = integer(schema.get("minLength"));
                Integer max = integer(schema.get("maxLength"));
                if (min != null || max != null) {
                    out.append("(?=.{").append(min == null ? 0 : min).append(',').append(max == null ? "" : max)
                            .append("}\\z)");
                    if (min != null) checked.add("minLength");
                    if (max != null) checked.add("maxLength");
                }
                if (schema.get("format") instanceof String f && FORMATS.containsKey(f)) {
                    out.append("(?=").append(FORMATS.get(f)).append("\\z)");
                    checked.add("format");
                }
                pattern = out.append(".*").toString();
            }
            default -> pattern = null;
        }
        for (String keyword : present) {
            (pattern != null && checked.contains(keyword) ? enforced : notEnforced).add(keyword);
        }
        return pattern;
    }

    /** A schema's {@code enum}, or its {@code const} as a list of one; {@code null} when it has neither. */
    private static List<Object> literals(Map<String, Object> schema) {
        if (schema.get("enum") instanceof List<?> values) return new ArrayList<>(values);
        if (schema.containsKey("const")) {
            List<Object> one = new ArrayList<>();
            one.add(schema.get("const"));
            return one;
        }
        return null;
    }

    /** A JSON value as a parameter carries it: a string as it is, anything else as JSON writes it. */
    private static String text(Object value) {
        if (value instanceof String s) return s;
        return Json.write(value).strip();
    }

    /** A schema's type, the first that is not {@code null} where it lists several. */
    private static String type(Object type) {
        if (type instanceof String s) return s;
        if (type instanceof List<?> types) {
            for (Object t : types) if (t instanceof String s && !s.equals("null")) return s;
        }
        return null;
    }

    private static Integer integer(Object value) {
        return value instanceof BigDecimal n ? n.intValue() : null;
    }
}
