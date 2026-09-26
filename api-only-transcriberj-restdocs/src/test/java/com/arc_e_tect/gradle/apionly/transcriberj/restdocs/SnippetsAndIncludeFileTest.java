package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.asciidoctor.log.LogRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T14.8: every test documents its case under {@code <prefix>/<operationId>/<caseId>/}; the
 * generated include file includes exactly the representatives, one per operation and status,
 * every snippet it names exists, and it renders without a missing include.
 */
@DisplayName("T14.8 Snippets and the include file")
class SnippetsAndIncludeFileTest {

    static final Map<String, GeneratedSuite.Run> RUNS = new HashMap<>();
    static Asciidoctor asciidoctor;

    @BeforeAll
    static void runEverySuite() {
        for (Fixtures.Contract contract : Fixtures.ALL) {
            GeneratedSuite suite = Suites.of(contract);
            try (ContractServer server = new ContractServer(ValidatingServer.of(suite))) {
                RUNS.put(contract.name(), suite.run(server));
            }
        }
        asciidoctor = Asciidoctor.Factory.create();
    }

    @AfterAll
    static void close() {
        asciidoctor.close();
    }

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    private static final Pattern INCLUDE = Pattern.compile("include::\\{invalid-request-snippets}/([^\\[]+)\\[]");

    private static List<String> includes(String include) {
        List<String> out = new ArrayList<>();
        Matcher m = INCLUDE.matcher(include);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    private static String include(GeneratedSuite suite) throws IOException {
        return Files.readString(suite.generated.resources()
                .resolve(InvalidRequestTests.includePath(suite.settings.contract())));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void everyCaseIsDocumentedUnderItsOwnDirectory(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        GeneratedSuite.Run run = RUNS.get(contract.name());
        for (GeneratedSuite.Case c : suite.cases) {
            Path directory = run.snippets().resolve(suite.snippetDirectory(InvalidRequestTests.PREFIX, c));
            assertThat(directory.resolve("http-request.adoc")).as(c.id()).exists();
            assertThat(directory.resolve("http-response.adoc")).as(c.id()).exists();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void theIncludeFileIncludesExactlyTheRepresentativesAndEveryPathExists(Fixtures.Contract contract)
            throws IOException {
        GeneratedSuite suite = Suites.of(contract);
        GeneratedSuite.Run run = RUNS.get(contract.name());
        List<String> included = includes(include(suite));

        List<String> directories = included.stream().map(p -> p.substring(0, p.lastIndexOf('/'))).distinct().toList();
        List<String> representatives = suite.cases.stream().filter(c -> c.json().get("representative").asBoolean())
                .map(c -> suite.snippetDirectory("", c).substring(1)).toList();
        assertThat(directories).containsExactlyElementsOf(representatives);
        assertThat(representatives).hasSize(suite.interfaces.size());
        for (String path : included) {
            assertThat(run.snippets().resolve(InvalidRequestTests.PREFIX).resolve(path)).exists();
        }
        List<String> kinds = included.stream().map(p -> p.substring(p.lastIndexOf('/') + 1)).distinct().toList();
        assertThat(kinds).contains("http-request.adoc", "http-response.adoc");
        if (contract != Fixtures.DEGRADED) assertThat(kinds).contains("response-fields.adoc");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void theIncludeFileRendersWithoutAMissingInclude(Fixtures.Contract contract) throws IOException {
        GeneratedSuite suite = Suites.of(contract);
        GeneratedSuite.Run run = RUNS.get(contract.name());
        List<LogRecord> logged = new ArrayList<>();
        org.asciidoctor.log.LogHandler handler = logged::add;
        asciidoctor.registerLogHandler(handler);
        String html;
        try {
            html = asciidoctor.convert(include(suite), Options.builder().safe(SafeMode.UNSAFE)
                    .attributes(Attributes.builder().attribute("snippets", run.snippets().toString()).build())
                    .build());
        } finally {
            asciidoctor.unregisterLogHandler(handler);
        }
        assertThat(logged).extracting(LogRecord::getMessage).isEmpty();
        assertThat(html).doesNotContain("Unresolved directive").contains("HTTP/1.1");
    }

    @Test
    void overridingThePrefixMovesTheSnippets() {
        GeneratedSuite suite = Suites.of(Fixtures.USER_ACCOUNT);
        GeneratedSuite.Run run;
        try (ContractServer server = new ContractServer(ValidatingServer.of(suite))) {
            run = suite.run(server, true, "moved");
        }

        assertThat(run.failed()).isEmpty();
        assertThat(run.snippets().resolve(InvalidRequestTests.PREFIX)).doesNotExist();
        for (GeneratedSuite.Case c : suite.cases) {
            assertThat(run.snippets().resolve(suite.snippetDirectory("moved", c)).resolve("http-request.adoc"))
                    .exists();
        }
    }
}
