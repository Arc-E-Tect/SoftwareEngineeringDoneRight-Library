package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.tools.Diagnostic;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T14.9 and T20.7: every generated test names its operation's {@code PATH}, calls {@code document(},
 * never {@code requestFields}, calls the hook its case's kind needs, and states no value of its own
 * -- its case is taken from {@code CASES} by position, and everything else from the case; an
 * interface declares {@code arrangeState} exactly when a case of its operation requires state; and
 * the generated sources compile with every lint on and nothing but notes.
 */
@DisplayName("T14.9, T20.7 Source shape")
class SourceShapeTest {

    private static final Pattern TEST = Pattern.compile(
            "    @Test\\n    @DisplayName\\((\"(?:[^\"\\\\]|\\\\.)*\")\\)\\n    default void (\\w+)\\(\\) \\{\\n(.*?)\\n    }\\n",
            Pattern.DOTALL);

    /** The paragraph every interface and the support class end their class Javadoc with. */
    static final String NOT_DOCUMENTATION = """
             *
             * <p>The snippets these tests write are a by-product of validation: {@code document()} is where
             * Spring REST Docs checks a response against its declared fields. They are not documentation,
             * and are not meant to be published.
            """;

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    /** Every test method of an interface: its display name literal, its name and its body. */
    static List<String[]> tests(String source) {
        List<String[]> out = new ArrayList<>();
        Matcher m = TEST.matcher(source);
        while (m.find()) out.add(new String[]{m.group(1), m.group(2), m.group(3)});
        return out;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void everyTestNamesPathCallsDocumentAndStatesNoValue(Fixtures.Contract contract) throws IOException {
        GeneratedSuite suite = Suites.of(contract);
        int count = 0;
        for (String tests : suite.interfaces) {
            String source = Files.readString(suite.source("com/example/contract/restdocs/" + tests + ".java"));
            String operation = tests.substring(0, tests.length() - ContractTests.SUFFIX.length()) + "Operation";
            for (String[] test : tests(source)) {
                String body = test[2];
                assertThat(body).as(test[1]).contains(".uri(" + operation + ".PATH, ").contains(".consumeWith(document(")
                        .doesNotContain("requestFields");
                assertThat(body).as(test[1]).doesNotContain("\"").doesNotContain("'");
                assertThat(body.replaceAll("CASES\\.get\\(\\d+\\)", "").replaceAll("\\w*\\d\\w*", ""))
                        .as(test[1] + " without its case's position").doesNotContainPattern("\\d");
                count++;
            }
            assertThat(source.split("@Test", -1).length - 1).isEqualTo(tests(source).size());
        }
        assertThat(count).isEqualTo(suite.cases.size());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void everyTestIsNamedFromItsCaseAndDisplaysWhatItChecks(Fixtures.Contract contract) throws IOException {
        GeneratedSuite suite = Suites.of(contract);
        for (GeneratedSuite.Case c : suite.cases) {
            String source = Files.readString(suite.source("com/example/contract/restdocs/" + c.tests() + ".java"));
            String[] test = tests(source).stream().filter(t -> t[1].equals(c.method())).findFirst().orElseThrow();
            String display = c.json().get("request").get("method").stringValue() + " "
                    + c.json().get("request").get("pathTemplate").stringValue() + ": "
                    + c.json().get("description").stringValue() + " returns " + c.json().get("expectedStatus").asInt();
            assertThat(test[0]).isEqualTo(ContractTests.literal(display));
            assertThat(test[2]).contains("CASES.get(" + c.index() + ")");
            assertThat(test[1]).isEqualTo(c.method()).endsWith("_returns" + c.json().get("expectedStatus").asInt());
            assertThat(test[2]).contains(c.requiresState() ? "        arrangeState(contractCase);\n"
                    : "        arrangeStatelessCase(contractCase);\n");
            if (!c.json().get("responseBodyClass").isNull()) {
                assertThat(test[2]).contains(c.json().get("responseBodyClass").stringValue() + "Docs.responseFields()");
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void everyInterfaceAndTheSupportClassSayTheirSnippetsAreNotDocumentation(Fixtures.Contract contract)
            throws IOException {
        GeneratedSuite suite = Suites.of(contract);
        List<String> classes = new ArrayList<>(suite.interfaces);
        classes.add(ContractTests.SUPPORT);
        for (String name : classes) {
            String source = Files.readString(suite.source("com/example/contract/restdocs/" + name + ".java"));
            assertThat(source).as(name).contains(NOT_DOCUMENTATION
                    + " */\n@com.arc_e_tect.sedr.utils.jacoco.marker.ExcludeFromJacocoGeneratedCodeCoverage");
            assertThat(source).as(name).doesNotContain("[documented]");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void onlyAnOperationWithAStatefulCaseHasAStateHook(Fixtures.Contract contract) throws IOException {
        GeneratedSuite suite = Suites.of(contract);
        for (String tests : suite.interfaces) {
            String source = Files.readString(suite.source("com/example/contract/restdocs/" + tests + ".java"));
            boolean stateful = suite.cases.stream().anyMatch(c -> c.tests().equals(tests) && c.requiresState());
            assertThat(source.contains("default void arrangeState(ContractCase contractCase) {")).as(tests)
                    .isEqualTo(stateful);
            assertThat(source).as(tests).contains("default void arrangeStatelessCase(ContractCase contractCase) {");
        }
        // The degraded corpus declares no success status, so none of its operations has a stateful case.
        assertThat(suite.stateful.isEmpty()).as("no operation with a stateful case").isEqualTo(contract == Fixtures.DEGRADED);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void theGeneratedSourcesCompileWithEveryLintAndOnlyNotes(Fixtures.Contract contract) {
        assertThat(Suites.of(contract).diagnostics).filteredOn(d -> d.getKind() != Diagnostic.Kind.NOTE)
                .extracting(Object::toString).isEmpty();
    }
}
