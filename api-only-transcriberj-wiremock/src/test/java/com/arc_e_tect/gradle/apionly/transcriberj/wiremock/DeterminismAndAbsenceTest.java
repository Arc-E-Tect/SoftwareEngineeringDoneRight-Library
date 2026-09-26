package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import com.arc_e_tect.gradle.apionly.transcriberj.restdocs.RestDocsEmitter;
import com.arc_e_tect.gradle.apionly.transcriberj.spi.Emitter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T15.14: generating twice gives byte-identical output, under another default {@code Locale} and
 * {@code TimeZone} too; a contract with no case gets no stubs and no file; and enabling this emitter
 * leaves every other emitter's output byte-identical.
 */
@DisplayName("T15.14 Determinism and absence")
class DeterminismAndAbsenceTest {

    @TempDir
    Path directory;

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void generatingTwiceGivesTheSameBytesWhateverTheLocale(Fixtures.Contract contract) {
        Map<String, String> first = files(contract, directory.resolve("first"), List.of(new WireMockEmitter()));
        Locale locale = Locale.getDefault();
        TimeZone zone = TimeZone.getDefault();
        Map<String, String> second;
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Chatham"));
            second = files(contract, directory.resolve("second"), List.of(new WireMockEmitter()));
        } finally {
            Locale.setDefault(locale);
            TimeZone.setDefault(zone);
        }
        assertThat(first).containsKey("sources/com/example/contract/wiremock/InvalidRequestStubs.java");
        assertThat(second).isEqualTo(first);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void enablingItLeavesTheOtherEmittersOutputAsItWas(Fixtures.Contract contract) {
        Map<String, String> without = files(contract, directory.resolve("without"), List.of(new RestDocsEmitter()));
        Map<String, String> with = files(contract, directory.resolve("with"),
                List.of(new RestDocsEmitter(), new WireMockEmitter()));
        with.keySet().removeIf(path -> path.startsWith("sources/com/example/contract/wiremock/"));
        assertThat(with).isEqualTo(without);
    }

    @Test
    void aContractWithoutCasesGetsNoStubs() throws Exception {
        // One operation declares no 400 although its input is constrained: a gap. The other declares
        // a 400 but constrains nothing.
        Path contract = directory.resolve("openapi.yaml");
        Files.writeString(contract, """
                openapi: 3.1.0
                info:
                  title: Without cases
                  version: 1.0.0
                paths:
                  /gap:
                    get:
                      operationId: getGap
                      parameters:
                        - {name: page, in: query, required: true, schema: {type: integer, minimum: 1}}
                      responses:
                        '200':
                          description: Fine.
                  /free:
                    get:
                      operationId: getFree
                      responses:
                        '200':
                          description: Fine.
                        '400':
                          description: Never, since nothing is constrained.
                """);
        Fixtures.Contract without = new Fixtures.Contract("without", null, List.of());
        Fixtures.Generated generated = Fixtures.generate(contract, without.settings(null, null),
                directory.resolve("out"), List.of(new WireMockEmitter()));
        assertThat(generated.sources().resolve("com/example/contract/wiremock")).doesNotExist();
        assertThat(Fixtures.Generated.files(generated.resources())).doesNotContainKey("wiremock");
        assertThat(Fixtures.Generated.files(generated.resources()).keySet()).noneMatch(p -> p.contains("wiremock"));
    }

    private static Map<String, String> files(Fixtures.Contract contract, Path into, List<Emitter> emitters) {
        boolean restdocs = emitters.stream().anyMatch(e -> e instanceof RestDocsEmitter);
        Fixtures.Generated generated = Fixtures.generate(contract.document(),
                contract.settings(restdocs ? Fixtures.TESTS_ON : null, null), into, emitters);
        Map<String, String> out = new TreeMap<>();
        Fixtures.Generated.files(generated.sources()).forEach((path, text) -> out.put("sources/" + path, text));
        Fixtures.Generated.files(generated.resources()).forEach((path, text) -> out.put("resources/" + path, text));
        return out;
    }
}
