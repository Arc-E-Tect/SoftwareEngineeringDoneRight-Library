package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T14.11: with rendering off -- no option, or {@code tests} anything but {@code true} -- the
 * emitter writes exactly what it wrote before rendering existed, byte for byte. The golden
 * files under {@code golden/<contract>/} were recorded from the emitter as it was then, and
 * are never re-recorded to make this test pass.
 */
@DisplayName("T14.11 Off means off")
class OffMeansOffTest {

    /** Set to the golden directory, as a path, to record it instead of comparing against it. */
    private static final String RECORD = "golden.record";

    @TempDir
    Path directory;

    static Stream<Arguments> off() {
        List<Arguments> out = new ArrayList<>();
        Map<String, String> falseOption = Map.of("tests", "false");
        for (Fixtures.Contract contract : Fixtures.ALL) {
            out.add(Arguments.of(contract, null));
            out.add(Arguments.of(contract, Map.of()));
            out.add(Arguments.of(contract, falseOption));
        }
        return out.stream();
    }

    @ParameterizedTest(name = "{0} with options {1}")
    @MethodSource("off")
    void theOutputIsByteIdenticalToTheOutputBeforeRendering(Fixtures.Contract contract, Map<String, String> options)
            throws IOException {
        Fixtures.Generated generated = Fixtures.generate(contract, options, directory);
        Map<String, String> actual = new HashMap<>();
        Fixtures.Generated.files(generated.sources().resolve("com/example/contract/restdocs"))
                .forEach((path, content) -> actual.put("sources/" + path, content));
        Fixtures.Generated.files(generated.resources()).forEach((path, content) -> actual.put("resources/" + path, content));

        String record = System.getProperty(RECORD);
        if (record != null && !record.isBlank()) {
            for (Map.Entry<String, String> file : actual.entrySet()) {
                Path target = Path.of(record, contract.name(), file.getKey() + ".golden");
                Files.createDirectories(target.getParent());
                Files.writeString(target, file.getValue());
            }
            return;
        }
        Map<String, String> golden = new HashMap<>();
        Path root = Path.of(resource("/golden/" + contract.name()));
        Fixtures.Generated.files(root).forEach((path, content) ->
                golden.put(path.substring(0, path.length() - ".golden".length()), content));
        assertThat(golden).as("the golden files of %s", contract.name()).isNotEmpty();
        assertThat(actual).isEqualTo(golden);
    }

    private static java.net.URI resource(String name) {
        try {
            return OffMeansOffTest.class.getResource(name).toURI();
        } catch (java.net.URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
