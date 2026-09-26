package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;

import javax.tools.Diagnostic;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T15.13: the generated stubs compile with {@code -Xlint:all} and every doclint check, reporting
 * nothing but notes; they call each response class's {@code requiredBody()} rather than embedding
 * a body, take every request value from the case, and hold no JSON literal; and their Javadoc says
 * what they are for and why they have a priority.
 */
@DisplayName("T15.13 Source shape and compilation")
class SourceShapeTest {

    private static final Pattern BODY_CASE = Pattern.compile("case \"([^\"]+)\" -> [\\w.]+\\.(\\w+)\\.requiredBody\\(\\);");

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void theStubsAreShapedAsPromised(Fixtures.Contract contract) throws IOException {
        GeneratedSuite suite = Suites.of(contract);
        String source = Files.readString(suite.source("com/example/contract/wiremock/InvalidRequestStubs.java"));

        assertThat(suite.diagnostics).filteredOn(d -> d.getSource() != null
                        && d.getSource().getName().contains("/wiremock/"))
                .allSatisfy(d -> assertThat(d.getKind()).as(d.toString()).isEqualTo(Diagnostic.Kind.NOTE));

        Set<String> answered = new TreeSet<>();
        for (GeneratedSuite.Case c : suite.cases) {
            JsonNode bodyClass = c.json().get("responseBodyClass");
            if (!bodyClass.isNull() && !c.json().get("expectedContentTypes").isEmpty()) answered.add(bodyClass.stringValue());
        }
        Set<String> called = new TreeSet<>();
        Matcher m = BODY_CASE.matcher(source);
        while (m.find()) {
            assertThat(m.group(2)).isEqualTo(m.group(1));
            called.add(m.group(1));
        }
        assertThat(called).isEqualTo(answered);

        assertThat(source).doesNotContain("\"{", "\"[", "withBody(\"");
        for (GeneratedSuite.Case c : suite.cases) {
            assertThat(source).as(c.id()).doesNotContain(c.id());
            JsonNode body = c.json().get("request").get("body");
            // A body as short as [] or 0 would be found in any Java source; the quotes above cover those.
            if (body != null && body.toString().length() > 4) assertThat(source).doesNotContain(body.toString());
        }

        assertThat(source).contains(" * WireMock stubs for the contract's invalid-request cases, so that the contract tests")
                .contains("It has priority {@value #PRIORITY},\n * so a project's own, looser stub for the same method and path");
    }
}
