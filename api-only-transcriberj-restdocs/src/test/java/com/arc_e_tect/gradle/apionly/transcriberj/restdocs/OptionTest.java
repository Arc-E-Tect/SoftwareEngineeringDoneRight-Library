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
        assertThat(InvalidRequestTests.enabled(Map.of("tests", "true"))).isTrue();
        assertThat(InvalidRequestTests.enabled(Map.of("tests", " TRUE "))).isTrue();
        assertThat(InvalidRequestTests.enabled(Map.of("tests", "yes"))).isFalse();
        assertThat(InvalidRequestTests.enabled(Map.of())).isFalse();
    }

    @Test
    void anUnknownOptionFailsGenerationNamingTheOptions() {
        assertThatThrownBy(() -> Fixtures.generate(Fixtures.USER_ACCOUNT, Map.of("test", "true"), directory))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The REST Docs emitter has no option 'test'; its only option is 'tests'.");
    }

    @Test
    void renderingWritesAnInterfacePerOperationWithCasesTheSupportClassAndTheIncludeFile() {
        Fixtures.Generated generated = Fixtures.generate(Fixtures.USER_ACCOUNT, Map.of("tests", "true"), directory);
        Path restdocs = generated.sources().resolve("com/example/contract/restdocs");

        assertThat(restdocs.resolve("InitiateUserRegistrationInvalidRequestContractTests.java")).exists();
        assertThat(restdocs.resolve("ResendVerificationEmailInvalidRequestContractTests.java")).exists();
        assertThat(restdocs.resolve("GetUserInvalidRequestContractTests.java")).doesNotExist();
        assertThat(restdocs.resolve("InvalidRequestContractSupport.java")).exists();
        assertThat(generated.resources().resolve("restdocs/user-account-invalid-requests.adoc")).exists();
    }

    @Test
    void aContractWithoutCasesGetsNoSupportClassAndNoIncludeFile() throws Exception {
        Path contract = directory.resolve("openapi.yaml");
        Files.writeString(contract, """
                openapi: 3.1.0
                info: {title: T, version: 1.0.0}
                paths:
                  /n:
                    get:
                      operationId: getN
                      responses:
                        '200':
                          description: found
                """);
        Fixtures.Generated generated = Fixtures.generate(contract, new Fixtures.Contract("none", null,
                java.util.List.of()).settings(Map.of("tests", "true")), directory, java.util.List.of(new RestDocsEmitter()));

        assertThat(generated.sources().resolve("com/example/contract/restdocs/InvalidRequestContractSupport.java"))
                .doesNotExist();
        assertThat(generated.resources().resolve("restdocs")).doesNotExist();
    }
}
