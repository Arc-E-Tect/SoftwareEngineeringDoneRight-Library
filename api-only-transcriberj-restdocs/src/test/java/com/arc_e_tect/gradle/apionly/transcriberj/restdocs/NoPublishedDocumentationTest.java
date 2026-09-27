package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T17a.1: generated tests verify; they do not document. With rendering on, for every fixture
 * contract, the emitter writes no file that publishes their snippets -- no resource at all.
 */
@DisplayName("T17a.1 No published documentation")
class NoPublishedDocumentationTest {

    @TempDir
    Path directory;

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void renderingWritesNoResource(Fixtures.Contract contract) {
        Fixtures.Generated generated = Fixtures.generate(contract, Map.of("tests", "true"), directory);

        assertThat(generated.sources().resolve("com/example/contract/restdocs/InvalidRequestContractSupport.java"))
                .as("the tests were rendered").exists();
        assertThat(generated.resources().resolve(RestDocsEmitter.ID)).doesNotExist();
        assertThat(Fixtures.Generated.files(generated.resources())).isEmpty();
    }
}
