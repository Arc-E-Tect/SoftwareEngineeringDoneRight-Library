package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T21.5: the fallbacks answer invalid requests. With the exact stubs removed, every body-located
 * invalid-request case's request is answered by the invalid fallback with the declared status --
 * a {@code format} too, where the README's table says its shape is checked. Every
 * parameter-located case's answer is what the README's table of what a fallback enforces says:
 * the invalid fallback where the violated keyword is enforced, the valid fallback where it is not.
 * The table is read from the README itself, so that it cannot drift from the behaviour. The
 * not-acceptable and unsupported-media-type cases reach their own fallbacks.
 */
@DisplayName("T21.5 Fallbacks answer invalid requests")
class FallbackInvalidTest {

    private static final Map<String, String> ENFORCED = table();

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void everyInvalidRequestIsAnsweredAsTheReadmeSays(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        try (DoubleServer server = Variations.fallbacksOnly(suite)) {
            for (GeneratedSuite.Case c : suite.cases("INVALID_REQUEST")) {
                server.forgetRequests();
                Replay.Response response = Replay.send(server, suite.request(c));
                String operation = suite.operationName(c.operation());
                boolean enforced = enforced(suite, c);
                String expected = operation + (enforced ? "/fallback-invalid" : "/fallback-valid");
                assertThat(server.journal().get(0).servedBy()).as("%s (%s %s)", c, c.field("in"), c.field("keyword"))
                        .isEqualTo(expected);
                if (enforced) assertThat(response.status()).as(c.toString()).isEqualTo(c.status());
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void theNegotiationCasesReachTheirOwnFallbacks(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        try (DoubleServer server = Variations.fallbacksOnly(suite)) {
            for (String kind : List.of("NOT_ACCEPTABLE", "UNSUPPORTED_MEDIA_TYPE")) {
                for (GeneratedSuite.Case c : suite.cases(kind)) {
                    server.forgetRequests();
                    Replay.Response response = Replay.send(server, suite.request(c));
                    String fallback = kind.equals("NOT_ACCEPTABLE") ? "not-acceptable" : "unsupported-media-type";
                    assertThat(server.journal().get(0).servedBy()).as(c.toString())
                            .isEqualTo(suite.operationName(c.operation()) + "/fallback-" + fallback);
                    assertThat(response.status()).as(c.toString()).isEqualTo(c.status());
                }
            }
        }
    }

    /** Both answers occur in the fixtures: the table is tested both ways. */
    @Test
    void theFixturesHaveEnforcedAndUnenforcedParameterCases() {
        List<Boolean> seen = new ArrayList<>();
        for (Fixtures.Contract contract : Fixtures.ALL) {
            GeneratedSuite suite = Suites.of(contract);
            for (GeneratedSuite.Case c : suite.cases("INVALID_REQUEST")) {
                if (!"body".equals(c.field("in"))) seen.add(enforced(suite, c));
            }
        }
        assertThat(seen).contains(true, false);
        assertThat(ENFORCED).containsKeys("required", "type", "pattern", "minimum", "format");
    }

    /** Whether the README says a fallback enforces what the case violates. */
    private static boolean enforced(GeneratedSuite suite, GeneratedSuite.Case c) {
        String in = c.field("in");
        String keyword = c.field("keyword");
        JsonNode schema;
        if ("body".equals(in)) {
            if (!"format".equals(keyword)) return true;
            schema = suite.oracle.resolve(Variations.bodyPointer(c.location(), suite.request(c).contentType()));
            for (String member : c.field("pointer").substring(1).split("/")) {
                schema = deref(suite, schema.has("properties") ? schema.get("properties").path(member)
                        : schema.path("items"));
            }
        } else {
            schema = Variations.parameterSchema(suite, c.location(), in, c.field("name"));
        }
        String name = c.field("name");
        if ("path".equals(in)) {
            Replay.Request request = suite.request(c);
            List<String> names = new ArrayList<>();
            Matcher placeholder = Pattern.compile("\\{([^}]+)}").matcher(request.pathTemplate());
            while (placeholder.find()) names.add(placeholder.group(1));
            String value = request.pathParameters().get(names.indexOf(name));
            if (Replay.encode(value).contains("%")) return false;
        }
        String rule = ENFORCED.get(keyword);
        assertThat(rule).as("the README's table has a row for %s", keyword).isNotNull();
        if (rule.startsWith("Always")) return true;
        if (rule.startsWith("Never")) return false;
        List<String> named = new ArrayList<>();
        Matcher code = Pattern.compile("`([^`]+)`").matcher(rule);
        while (code.find()) named.add(code.group(1));
        String subject = keyword.equals("format") ? schema.path("format").stringValue("")
                : schema.path("type").stringValue("");
        return named.contains(subject);
    }

    /** A schema, its {@code $ref} into the contract followed. */
    private static JsonNode deref(GeneratedSuite suite, JsonNode schema) {
        JsonNode ref = schema.path("$ref");
        return ref.isString() ? suite.oracle.resolve(ref.stringValue().substring(1)) : schema;
    }

    /** The README's table of what a fallback enforces: each keyword's rule. */
    private static Map<String, String> table() {
        try {
            String readme = Files.readString(Path.of("README.adoc"));
            String table = readme.substring(readme.indexOf("// tag::fallback-enforces[]"),
                    readme.indexOf("// end::fallback-enforces[]"));
            assertThat(readme).contains("A path value that travels percent-encoded");
            Map<String, String> out = new LinkedHashMap<>();
            Matcher row = Pattern.compile("\\n\\|`([^`]+)`\\n\\|([^\\n]+)").matcher(table);
            while (row.find()) out.put(row.group(1), row.group(2));
            return out;
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
