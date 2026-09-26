package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import com.arc_e_tect.gradle.apionly.transcriberj.core.Generation;
import com.arc_e_tect.gradle.apionly.transcriberj.core.GenerationReport;
import com.arc_e_tect.gradle.apionly.transcriberj.spi.Emitter;
import com.arc_e_tect.gradle.apionly.transcriberj.spi.Settings;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/** The contracts the emitter is tested over, each with the settings it is written for, and their generation. */
final class Fixtures {

    static final String PACKAGE = "com.example.contract";

    /**
     * One contract and how it is generated.
     *
     * @param name     what the tests call it
     * @param resource the contract, as a test resource
     * @param formats  the formats an invalid-request case is derived for
     */
    record Contract(String name, String resource, List<String> formats) {

        /** The contract document. */
        Path document() {
            try {
                return Path.of(Fixtures.class.getResource(resource).toURI());
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }

        /** The settings, with the emitter options given. */
        Settings settings(Map<String, String> restdocsOptions) {
            Map<String, Map<String, String>> options = restdocsOptions == null ? Map.of()
                    : Map.of(RestDocsEmitter.ID, restdocsOptions);
            return new Settings(name, PACKAGE, false, "PLACEHOLDER", 2, null, "400", true, formats, options);
        }
    }

    static final Contract USER_ACCOUNT = new Contract("user-account", "/contracts/user-account/openapi.yaml", List.of());
    static final Contract KEYWORDS = new Contract("keywords", "/contracts/corpus/keywords.yaml", List.of("date", "uuid"));
    static final Contract DEGRADED = new Contract("degraded", "/contracts/corpus/degraded.yaml", List.of());
    static final Contract TRANSMISSION = new Contract("transmission", "/contracts/transmission.yaml", List.of());

    /** Every contract. */
    static final List<Contract> ALL = List.of(USER_ACCOUNT, KEYWORDS, DEGRADED, TRANSMISSION);

    /**
     * One generation: where its sources and resources went, and its report.
     *
     * @param sources   the source root
     * @param resources the resource root
     * @param index     the endpoint index
     * @param report    the report
     */
    record Generated(Path sources, Path resources, Path index, GenerationReport report) {

        /** Every file generated into a directory, by its path relative to it. */
        static Map<String, String> files(Path root) {
            Map<String, String> out = new TreeMap<>();
            if (!Files.isDirectory(root)) return out;
            try (Stream<Path> walk = Files.walk(root)) {
                for (Path p : walk.filter(Files::isRegularFile).toList()) {
                    out.put(root.relativize(p).toString().replace('\\', '/'), Files.readString(p));
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return out;
        }
    }

    private Fixtures() {
    }

    /** Generates a contract into a directory, with this emitter and the options given. */
    static Generated generate(Contract contract, Map<String, String> options, Path into) {
        return generate(contract.document(), contract.settings(options), into, List.of(new RestDocsEmitter()));
    }

    /** Generates a contract document with the settings and emitters given. */
    static Generated generate(Path document, Settings settings, Path into, List<Emitter> emitters) {
        Path sources = into.resolve("sources");
        Path resources = into.resolve("resources");
        Path index = into.resolve("contract-endpoints.properties");
        GenerationReport report = Generation.run(document, "1.0.0", "x", settings, sources, resources, emitters,
                index);
        return new Generated(sources, resources, index, report);
    }
}
