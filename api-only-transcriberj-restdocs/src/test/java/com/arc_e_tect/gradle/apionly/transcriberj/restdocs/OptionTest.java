package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The {@code tests} option: {@code true}, in any case, switches rendering on; any other option is a mistake. */
@DisplayName("The tests option")
class OptionTest {

    @TempDir
    Path directory;

    @Test
    void trueInAnyCaseSwitchesRenderingOn() {
        assertThat(ContractTests.enabled(Map.of("tests", "true"))).isTrue();
        assertThat(ContractTests.enabled(Map.of("tests", " TRUE "))).isTrue();
        assertThat(ContractTests.enabled(Map.of("tests", "yes"))).isFalse();
        assertThat(ContractTests.enabled(Map.of())).isFalse();
    }

    @Test
    void anUnknownOptionFailsGenerationNamingTheOptions() {
        assertThatThrownBy(() -> Fixtures.generate(Fixtures.USER_ACCOUNT, Map.of("test", "true"), directory))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The REST Docs emitter has no option 'test'; its only option is 'tests'.");
    }

    @Test
    void renderingWritesAnInterfacePerOperationWithCasesTheSupportClassAndTheException() {
        Fixtures.Generated generated = Fixtures.generate(Fixtures.USER_ACCOUNT, Map.of("tests", "true"), directory);
        Path restdocs = generated.sources().resolve("com/example/contract/restdocs");

        assertThat(restdocs.resolve("InitiateUserRegistrationContractTests.java")).exists();
        assertThat(restdocs.resolve("ResendVerificationEmailContractTests.java")).exists();
        assertThat(restdocs.resolve("GetUserContractTests.java")).exists();
        assertThat(restdocs.resolve("ContractTestSupport.java")).exists();
        assertThat(restdocs.resolve("FixtureNotImplementedException.java")).exists();
        try (var files = java.nio.file.Files.list(restdocs)) {
            assertThat(files.map(p -> p.getFileName().toString())).noneMatch(n -> n.contains("InvalidRequestContract"));
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        assertThat(generated.resources().resolve("restdocs")).doesNotExist();
    }

    @Test
    void theEmitterDeclaresItWritesJavaOnly() {
        assertThat(new RestDocsEmitter().produces(Map.of())).containsExactly(
                com.arc_e_tect.gradle.apionly.transcriberj.spi.Output.JAVA);
        assertThat(new RestDocsEmitter().produces(Map.of("tests", "true"))).containsExactly(
                com.arc_e_tect.gradle.apionly.transcriberj.spi.Output.JAVA);
    }

    @Test
    void aContractWithoutCasesGetsNoSupportClassAndNoException() throws Exception {
        Path contract = directory.resolve("openapi.yaml");
        Files.writeString(contract, """
                openapi: 3.1.0
                info: {title: T, version: 1.0.0}
                paths:
                  /n:
                    get:
                      operationId: getN
                      responses:
                        '500':
                          description: failed
                """);
        Fixtures.Generated generated = Fixtures.generate(contract, new Fixtures.Contract("none", null,
                java.util.List.of()).settings(Map.of("tests", "true")), directory, java.util.List.of(new RestDocsEmitter()));

        assertThat(generated.sources().resolve("com/example/contract/restdocs/ContractTestSupport.java"))
                .doesNotExist();
        assertThat(generated.sources().resolve("com/example/contract/restdocs/FixtureNotImplementedException.java"))
                .doesNotExist();
        assertThat(generated.resources().resolve("restdocs")).doesNotExist();
    }
}
