package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import com.arc_e_tect.gradle.apionly.transcriberj.core.Generation;
import com.arc_e_tect.gradle.apionly.transcriberj.spi.ManagedDependency;
import com.arc_e_tect.gradle.apionly.transcriberj.spi.Output;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T21.15 and T21.16: the options, and what the emitter declares. An unknown option, a bad
 * {@code format}, and a {@code priority} below 1 or not a whole number each fail generation with a
 * clear message; {@code priority} sets the exact stubs' priority and moves the fallbacks after it;
 * switching the fallbacks off removes exactly the fallback files. The emitter declares files alone
 * in the {@code files} format and Java alone in the {@code java} format, so the plugin gives it no
 * source set -- and no managed dependency -- unless it writes Java.
 */
@DisplayName("T21.15 Options; T21.16 Dependency")
class OptionsTest {

    @TempDir
    Path directory;

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @Test
    void aMistakeFailsGenerationSayingWhatIsExpected() {
        record Mistake(Map<String, String> options, String message) {
        }
        for (Mistake m : List.of(
                new Mistake(Map.of("prority", "2"), "has no option 'prority'; its options are 'format', 'priority', 'fallbacks'"),
                new Mistake(Map.of("format", "yaml"), "'format' is 'yaml'; it must be 'files'"),
                new Mistake(Map.of("priority", "0"), "'priority' is '0'; it must be a whole number of 1 or more"),
                new Mistake(Map.of("priority", "two"), "'priority' is 'two'"),
                new Mistake(Map.of("priority", "1.5"), "'priority' is '1.5'"),
                new Mistake(Map.of("fallbacks", "no"), "'fallbacks' is 'no'; it must be 'true'"))) {
            Path into = directory.resolve("mistake" + m.hashCode());
            assertThatThrownBy(() -> Generation.runEmitter(Generation.derive(Fixtures.RESPONSES.document(), null,
                            "1.0.0", "x", Fixtures.RESPONSES.settings(null, m.options())), new WireMockEmitter(),
                    into.resolve("java"), into.resolve("resources"), into.resolve("files")))
                    .as(m.options().toString()).hasMessageContaining(m.message());
            assertThatThrownBy(() -> new WireMockEmitter().produces(m.options())).as(m.options().toString())
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(m.message());
        }
    }

    @Test
    void theOptionsAreReadAsWritten() {
        assertThat(Options.of(Map.of())).isEqualTo(new Options(Options.Format.FILES, 1, true));
        assertThat(Options.of(Map.of("format", " JAVA ", "priority", " 3 ", "fallbacks", "False")))
                .isEqualTo(new Options(Options.Format.JAVA, 3, false));
    }

    @Test
    void theEmitterDeclaresWhatTheFormatWrites() {
        WireMockEmitter emitter = new WireMockEmitter();
        assertThat(emitter.produces(Map.of())).containsExactly(Output.FILES);
        assertThat(emitter.produces(Map.of("format", "files"))).containsExactly(Output.FILES);
        assertThat(emitter.produces(Map.of("format", "java"))).containsExactly(Output.JAVA);
        assertThat(emitter.id()).isEqualTo("wiremock");
        assertThat(emitter.represents()).isEmpty();
        assertThat(emitter.dependencies()).containsExactly(
                new ManagedDependency("org.wiremock", "wiremock-jetty12", "3.13.2", "4"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void thePrioritySetsTheExactStubsAndMovesTheFallbacks(Fixtures.Contract contract) {
        GeneratedSuite suite = GeneratedSuite.of(contract, Map.of("priority", "3"), Suites.directory("priority"));
        Set<Integer> exact = suite.stubMappings().values().stream().filter(m -> !m.getName().contains("/fallback-"))
                .map(m -> m.getPriority()).collect(Collectors.toSet());
        assertThat(exact).containsExactly(3);
        assertThat(suite.stubMappings().values()).filteredOn(m -> m.getName().contains("/fallback-"))
                .allSatisfy(m -> assertThat(m.getPriority()).isGreaterThan(3));
        for (GeneratedSuite.Case c : suite.cases) assertThat(suite.mapping(c).build().getPriority()).isEqualTo(3);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void switchingTheFallbacksOffRemovesExactlyTheFallbackFiles(Fixtures.Contract contract) {
        Map<String, String> on = Fixtures.Generated.files(
                Fixtures.generate(contract, Map.of(), directory.resolve("on")).files());
        Fixtures.Generated off = Fixtures.generate(contract, Map.of("fallbacks", "false"), directory.resolve("off"));
        Map<String, String> without = Fixtures.Generated.files(off.files());
        on.keySet().removeIf(path -> path.contains("/fallback-"));
        on.keySet().removeIf(path -> path.startsWith(WireMockEmitter.FILES + "/") && !without.containsKey(path));
        assertThat(without).isEqualTo(on);
        assertThat(without.keySet()).noneMatch(path -> path.contains("fallback"));
    }

    @Test
    void aContractWithNothingToStubGetsNothing() throws Exception {
        Path contract = directory.resolve("openapi.yaml");
        Files.writeString(contract, """
                openapi: 3.1.0
                info:
                  title: Nothing to stub
                  version: 1.0.0
                paths: {}
                """);
        Fixtures.Contract nothing = new Fixtures.Contract("nothing", null, List.of());
        Fixtures.Generated generated = Fixtures.generate(contract, nothing, null, Map.of(), directory.resolve("out"));
        assertThat(Fixtures.Generated.files(generated.files())).isEmpty();
        assertThat(Fixtures.Generated.files(generated.java())).isEmpty();
    }
}
