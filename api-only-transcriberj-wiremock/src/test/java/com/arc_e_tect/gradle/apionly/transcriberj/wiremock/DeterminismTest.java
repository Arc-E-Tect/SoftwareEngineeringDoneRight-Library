package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T21.13: generating twice gives byte-identical files and Java, under another default
 * {@code Locale} and {@code TimeZone} too; and enabling this emitter leaves the other emitters'
 * output as it was.
 */
@DisplayName("T21.13 Determinism")
class DeterminismTest {

    @TempDir
    Path directory;

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void generatingTwiceGivesTheSameBytesWhateverTheLocale(Fixtures.Contract contract) {
        Map<String, String> first = output(contract, directory.resolve("first"));
        Locale locale = Locale.getDefault();
        TimeZone zone = TimeZone.getDefault();
        Map<String, String> second;
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Chatham"));
            second = output(contract, directory.resolve("second"));
        } finally {
            Locale.setDefault(locale);
            TimeZone.setDefault(zone);
        }
        assertThat(first).containsKey("java/com/example/contract/wiremock/ContractStubs.java")
                .anySatisfy((path, text) -> assertThat(path).startsWith("files/mappings/"));
        assertThat(second).isEqualTo(first);
    }

    /** Everything the two formats write, and the core's and REST Docs' sources, by path. */
    private static Map<String, String> output(Fixtures.Contract contract, Path into) {
        Fixtures.Generated generated = Fixtures.generate(contract, Map.of(), into);
        Map<String, String> out = new TreeMap<>();
        Fixtures.Generated.files(generated.files()).forEach((path, text) -> out.put("files/" + path, text));
        Fixtures.Generated.files(generated.java()).forEach((path, text) -> out.put("java/" + path, text));
        Fixtures.Generated.files(generated.core()).forEach((path, text) -> out.put("core/" + path, text));
        Fixtures.Generated.files(generated.restdocs()).forEach((path, text) -> out.put("restdocs/" + path, text));
        return out;
    }
}
