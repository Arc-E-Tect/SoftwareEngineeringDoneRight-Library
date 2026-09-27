package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import com.arc_e_tect.gradle.apionly.transcriberj.core.Generation;
import com.arc_e_tect.gradle.apionly.transcriberj.core.GenerationReport;
import com.arc_e_tect.gradle.apionly.transcriberj.restdocs.RestDocsEmitter;
import com.arc_e_tect.gradle.apionly.transcriberj.spi.Settings;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/** The contracts the emitter is tested over, each with the settings it is written for, and their generation. */
final class Fixtures {

    static final String PACKAGE = "com.example.contract";

    /** The REST Docs emitter's option that renders the cases as tests, on. */
    static final Map<String, String> TESTS_ON = Map.of("tests", "true");

    /**
     * One contract and how it is generated.
     *
     * @param name     what the tests call it
     * @param resource the contract, as a test resource
     * @param formats  the formats a case is derived for, and the stubs check
     * @param strict   whether {@code strictRequests} is on
     */
    record Contract(String name, String resource, List<String> formats, boolean strict) {

        Contract(String name, String resource, List<String> formats) {
            this(name, resource, formats, true);
        }

        /** The contract document. */
        Path document() {
            try {
                return Path.of(Fixtures.class.getResource(resource).toURI());
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }

        /** The settings, with each emitter's options given; null for none. */
        Settings settings(Map<String, String> restdocs, Map<String, String> wiremock) {
            Map<String, Map<String, String>> options = new HashMap<>();
            if (restdocs != null) options.put(RestDocsEmitter.ID, restdocs);
            if (wiremock != null) options.put(WireMockEmitter.ID, wiremock);
            return new Settings(name, PACKAGE, false, "PLACEHOLDER", 2, null, "400", strict, formats, options);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static final Contract USER_ACCOUNT = new Contract("user-account", "/contracts/user-account/openapi.yaml", List.of());
    static final Contract KEYWORDS = new Contract("keywords", "/contracts/corpus/keywords.yaml", List.of("date", "uuid"));
    static final Contract DEGRADED = new Contract("degraded", "/contracts/corpus/degraded.yaml", List.of());
    static final Contract TRANSMISSION = new Contract("transmission", "/contracts/transmission.yaml", List.of());
    static final Contract RESPONSES = new Contract("responses", "/contracts/responses.yaml", List.of());
    static final Contract SUCCESS = new Contract("success", "/contracts/cases/success.yaml", List.of());
    static final Contract NOT_FOUND = new Contract("not-found", "/contracts/cases/not-found.yaml", List.of());
    static final Contract NEGOTIATION = new Contract("negotiation", "/contracts/cases/negotiation.yaml", List.of());
    static final Contract COVERAGE = new Contract("coverage", "/contracts/cases/coverage.yaml", List.of());
    static final Contract PRECEDENCE = new Contract("precedence", "/contracts/precedence.yaml", List.of());
    static final Contract UNBUILDABLE = new Contract("unbuildable", "/contracts/unbuildable.yaml", List.of());

    /** The keyword corpus with no format named in {@code validateFormats}. */
    static final Contract KEYWORDS_UNCHECKED_FORMATS = new Contract("keywords", "/contracts/corpus/keywords.yaml",
            List.of());

    /** The user-account contract with {@code strictRequests} off. */
    static final Contract USER_ACCOUNT_LENIENT = new Contract("user-account", "/contracts/user-account/openapi.yaml",
            List.of(), false);

    /** Every contract, each as it is written to be generated. */
    static final List<Contract> ALL = List.of(USER_ACCOUNT, KEYWORDS, DEGRADED, TRANSMISSION, RESPONSES, SUCCESS,
            NOT_FOUND, NEGOTIATION, COVERAGE, PRECEDENCE);

    /**
     * One generation: where each emitter's output went, and the reports.
     *
     * @param core      the core's source root
     * @param restdocs  the REST Docs emitter's source root
     * @param java      the WireMock emitter's source root, in the {@code java} format
     * @param files     the WireMock emitter's files directory, in the {@code files} format
     * @param resources the core's resource root
     * @param report    the core's report
     * @param wiremock  the WireMock emitter's report, in the {@code files} format
     */
    record Generated(Path core, Path restdocs, Path java, Path files, Path resources, GenerationReport report,
                     GenerationReport wiremock) {

        /** Every source root. */
        List<Path> sources() {
            return List.of(core, restdocs, java);
        }

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

    /**
     * Generates a contract document: the core, the REST Docs emitter with the options given, and the
     * WireMock emitter twice -- with its options as files, and with them and {@code format = 'java'} as
     * Java -- each into directories of its own, as the plugin's tasks do.
     *
     * @param document the contract
     * @param contract the fixture whose settings it is generated with
     * @param restdocs the REST Docs emitter's options, or null to leave it out
     * @param wiremock the WireMock emitter's options, without {@code format}
     * @param into     where to generate
     * @return the generation
     */
    static Generated generate(Path document, Contract contract, Map<String, String> restdocs,
                              Map<String, String> wiremock, Path into) {
        Map<String, String> files = new HashMap<>(wiremock);
        files.remove(Options.FORMAT);
        Map<String, String> java = new HashMap<>(files);
        java.put(Options.FORMAT, "java");
        Generation.Derivation derivation = Generation.derive(document, null, "1.0.0", "x",
                contract.settings(restdocs, files));
        Generation.writeCore(derivation, into.resolve("core"), into.resolve("resources"),
                into.resolve("contract-endpoints.properties"));
        if (restdocs != null) {
            Generation.runEmitter(derivation, new RestDocsEmitter(), into.resolve("restdocs"),
                    into.resolve("restdocs-resources"), into.resolve("restdocs-files"));
        }
        GenerationReport report = Generation.runEmitter(derivation, new WireMockEmitter(),
                into.resolve("files-java"), into.resolve("files-resources"), into.resolve("files"));
        Generation.Derivation javaDerivation = Generation.derive(document, null, "1.0.0", "x",
                contract.settings(restdocs, java));
        Generation.runEmitter(javaDerivation, new WireMockEmitter(), into.resolve("java"),
                into.resolve("java-resources"), into.resolve("java-files"));
        return new Generated(into.resolve("core"), into.resolve("restdocs"), into.resolve("java"),
                into.resolve("files"), into.resolve("resources"), derivation.report(), report);
    }

    /** A fixture contract, generated with the REST Docs tests and the WireMock options given. */
    static Generated generate(Contract contract, Map<String, String> wiremock, Path into) {
        return generate(contract.document(), contract, TESTS_ON, wiremock, into);
    }
}
