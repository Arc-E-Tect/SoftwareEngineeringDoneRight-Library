package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T17a.4: with rendering on, every generated source is what it was before the include file was
 * dropped, except for the two changes that came with it: the paragraph saying the snippets are
 * not documentation, and the ` [documented]` a display name no longer ends in. The golden
 * files under {@code golden-rendered/<contract>/} were recorded from the emitter before that
 * change, and are never re-recorded to make this test pass.
 */
@DisplayName("T17a.4 Rendered output unchanged")
class RenderedOutputUnchangedTest {

    /** Set to the golden directory, as a path, to record it instead of comparing against it. */
    private static final String RECORD = "golden.rendered.record";

    /** The paragraph the interfaces and the support class gained, in their class Javadoc. */
    static final String NOT_DOCUMENTATION = """
             *
             * <p>The snippets these tests write are a by-product of validation: {@code document()} is where
             * Spring REST Docs checks a response against its declared fields. They are not documentation,
             * and are not meant to be published.
            """;

    @TempDir
    Path directory;

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void everySourceIsUnchangedButForTheNotDocumentationParagraphAndTheDocumentedSuffix(Fixtures.Contract contract)
            throws IOException {
        Fixtures.Generated generated = Fixtures.generate(contract, Map.of("tests", "true"), directory);
        Map<String, String> actual = Fixtures.Generated.files(generated.sources().resolve("com/example/contract/restdocs"));

        String record = System.getProperty(RECORD);
        if (record != null && !record.isBlank()) {
            for (Map.Entry<String, String> file : actual.entrySet()) {
                Path target = Path.of(record, contract.name(), file.getKey() + ".golden");
                Files.createDirectories(target.getParent());
                Files.writeString(target, file.getValue());
            }
            return;
        }
        Map<String, String> golden = new TreeMap<>();
        Fixtures.Generated.files(Path.of(resource("/golden-rendered/" + contract.name()))).forEach((path, content) ->
                golden.put(path.substring(0, path.length() - ".golden".length()), normalized(content)));
        Map<String, String> normalizedActual = new TreeMap<>();
        actual.forEach((path, content) -> normalizedActual.put(path, normalized(content)));

        assertThat(golden).as("the golden files of %s", contract.name()).isNotEmpty();
        assertThat(normalizedActual).isEqualTo(golden);
    }

    /** A source without the two intended changes, whichever side of them it was generated on. */
    static String normalized(String source) {
        return source.replace(NOT_DOCUMENTATION, "").replace(" [documented]\")", "\")");
    }

    private static java.net.URI resource(String name) {
        try {
            return RenderedOutputUnchangedTest.class.getResource(name).toURI();
        } catch (java.net.URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
