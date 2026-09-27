package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON, read and written without a library: the emitter runs in Gradle's JVM, where it must not
 * bring one. Objects are {@link Map}s keeping their members' order, arrays {@link List}s, numbers
 * {@link BigDecimal}s, and {@code true}, {@code false} and {@code null} themselves.
 */
final class Json {

    private final String text;
    private int at;

    private Json(String text) {
        this.text = text;
    }

    /**
     * A JSON document, read.
     *
     * @param text the document
     * @return its value
     * @throws IllegalArgumentException when the text is not JSON
     */
    static Object read(String text) {
        Json json = new Json(text);
        Object value = json.value();
        json.space();
        if (json.at != text.length()) throw json.error("end of document");
        return value;
    }

    /**
     * A value written as JSON, each member on a line of its own and indented two spaces, ending
     * with a newline: the same text for the same value, on any machine.
     *
     * @param value the value
     * @return the document
     */
    static String write(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out, "");
        return out.append('\n').toString();
    }

    /**
     * A value written as JSON on one line, without whitespace: as the TranscriberJ writes a schema.
     *
     * @param value the value
     * @return the text
     */
    static String compact(Object value) {
        StringBuilder out = new StringBuilder();
        compact(value, out);
        return out.toString();
    }

    private static void compact(Object value, StringBuilder out) {
        if (value instanceof Map<?, ?> map) {
            out.append('{');
            int i = 0;
            for (Map.Entry<?, ?> member : map.entrySet()) {
                if (i++ > 0) out.append(',');
                string(String.valueOf(member.getKey()), out);
                out.append(':');
                compact(member.getValue(), out);
            }
            out.append('}');
        } else if (value instanceof List<?> list) {
            out.append('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) out.append(',');
                compact(list.get(i), out);
            }
            out.append(']');
        } else {
            write(value, out, "");
        }
    }

    private static void write(Object value, StringBuilder out, String indent) {
        String inner = indent + "  ";
        if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                out.append("{}");
                return;
            }
            out.append("{\n");
            int i = 0;
            for (Map.Entry<?, ?> member : map.entrySet()) {
                out.append(inner);
                string(String.valueOf(member.getKey()), out);
                out.append(": ");
                write(member.getValue(), out, inner);
                out.append(++i < map.size() ? ",\n" : "\n");
            }
            out.append(indent).append('}');
        } else if (value instanceof List<?> list) {
            if (list.isEmpty()) {
                out.append("[]");
                return;
            }
            out.append("[\n");
            for (int i = 0; i < list.size(); i++) {
                out.append(inner);
                write(list.get(i), out, inner);
                out.append(i + 1 < list.size() ? ",\n" : "\n");
            }
            out.append(indent).append(']');
        } else if (value instanceof String s) {
            string(s, out);
        } else if (value instanceof BigDecimal n) {
            out.append(n.toString());
        } else {
            // Integer, Boolean or null: their own text.
            out.append(value);
        }
    }

    private static void string(String s, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    // ------------------------------------------------------------------- reading

    private Object value() {
        space();
        if (at >= text.length()) throw error("a value");
        char c = text.charAt(at);
        return switch (c) {
            case '{' -> object();
            case '[' -> array();
            case '"' -> string();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> number();
        };
    }

    private Map<String, Object> object() {
        Map<String, Object> out = new LinkedHashMap<>();
        at++;
        space();
        if (peek('}')) return out;
        do {
            space();
            if (at >= text.length() || text.charAt(at) != '"') throw error("a member name");
            String name = string();
            space();
            expect(':');
            out.put(name, value());
            space();
        } while (peek(','));
        expect('}');
        return out;
    }

    private List<Object> array() {
        List<Object> out = new ArrayList<>();
        at++;
        space();
        if (peek(']')) return out;
        do {
            out.add(value());
            space();
        } while (peek(','));
        expect(']');
        return out;
    }

    private String string() {
        StringBuilder out = new StringBuilder();
        at++;
        while (at < text.length()) {
            char c = text.charAt(at++);
            if (c == '"') return out.toString();
            if (c != '\\') {
                out.append(c);
                continue;
            }
            if (at >= text.length()) break;
            char e = text.charAt(at++);
            switch (e) {
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'u' -> {
                    if (at + 4 > text.length()) throw error("four hexadecimal digits");
                    out.append((char) Integer.parseInt(text.substring(at, at + 4), 16));
                    at += 4;
                }
                default -> out.append(e);
            }
        }
        throw error("the end of a string");
    }

    private Object literal(String word, Object value) {
        if (!text.startsWith(word, at)) throw error(word);
        at += word.length();
        return value;
    }

    private BigDecimal number() {
        int start = at;
        while (at < text.length() && "+-0123456789.eE".indexOf(text.charAt(at)) >= 0) at++;
        try {
            return new BigDecimal(text.substring(start, at));
        } catch (NumberFormatException e) {
            at = start;
            throw error("a value");
        }
    }

    private void space() {
        while (at < text.length() && Character.isWhitespace(text.charAt(at))) at++;
    }

    private boolean peek(char c) {
        if (at < text.length() && text.charAt(at) == c) {
            at++;
            return true;
        }
        return false;
    }

    private void expect(char c) {
        if (!peek(c)) throw error("'" + c + "'");
    }

    private IllegalArgumentException error(String expected) {
        return new IllegalArgumentException("Expected " + expected + " at offset " + at + " of " + text);
    }
}
