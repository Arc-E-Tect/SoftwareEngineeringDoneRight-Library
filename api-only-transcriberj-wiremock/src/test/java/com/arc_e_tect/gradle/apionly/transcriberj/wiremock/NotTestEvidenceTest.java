package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import com.arc_e_tect.gradle.detector.core.scan.PropertyResolutionContext;
import com.arc_e_tect.gradle.doppelganger.detect.VerifiedContractTest;
import com.arc_e_tect.gradle.doppelganger.scan.RestDocsScanner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T15.11: generated stubs are not test evidence. The contract-evidence scan the API-Only Suite
 * runs over test sources -- the Doppelganger API Detector's, reading them without a classpath and
 * resolving {@code ClassName.PATH} through the TranscriberJ's endpoint index -- finds no conformance
 * test in the generated stubs alone, and, over the rendered REST Docs tests and the stubs together,
 * exactly what it finds in the rendered tests alone.
 */
@DisplayName("T15.11 Generated stubs are not test evidence")
class NotTestEvidenceTest {

    @TempDir
    Path directory;

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void theStubsAddNothingToWhatTheScanCounts(Fixtures.Contract contract) throws IOException {
        GeneratedSuite suite = Suites.of(contract);
        RestDocsScanner scanner = new RestDocsScanner("", PropertyResolutionContext.of(index(suite), Set.of()));
        Path wiremock = suite.source("com/example/contract/wiremock");
        Path restdocs = suite.source("com/example/contract/restdocs");
        Path both = directory.resolve(contract.name());
        copy(wiremock, both.resolve("wiremock"));
        copy(restdocs, both.resolve("restdocs"));

        List<VerifiedContractTest> stubsAlone = scanner.scanWithStatusCodes(wiremock.toFile());
        List<VerifiedContractTest> testsAlone = scanner.scanWithStatusCodes(restdocs.toFile());
        List<VerifiedContractTest> together = scanner.scanWithStatusCodes(both.toFile());

        assertThat(stubsAlone).isEmpty();
        assertThat(testsAlone).hasSize(suite.cases.size());
        assertThat(together).hasSameSizeAs(testsAlone);
        assertThat(together.stream().map(NotTestEvidenceTest::key).sorted().toList())
                .isEqualTo(testsAlone.stream().map(NotTestEvidenceTest::key).sorted().toList());
    }

    private static String key(VerifiedContractTest test) {
        return test.endpoint().verb() + " " + test.endpoint().path() + " " + test.statusCode();
    }

    private static Map<String, String> index(GeneratedSuite suite) throws IOException {
        Properties index = new Properties();
        try (Reader in = Files.newBufferedReader(suite.generated.index(), StandardCharsets.ISO_8859_1)) {
            index.load(in);
        }
        Map<String, String> properties = new TreeMap<>();
        index.forEach((k, v) -> properties.put((String) k, (String) v));
        return properties;
    }

    private static void copy(Path from, Path to) throws IOException {
        Files.createDirectories(to);
        try (Stream<Path> files = Files.list(from)) {
            for (Path f : files.toList()) Files.copy(f, to.resolve(f.getFileName()));
        }
    }
}
