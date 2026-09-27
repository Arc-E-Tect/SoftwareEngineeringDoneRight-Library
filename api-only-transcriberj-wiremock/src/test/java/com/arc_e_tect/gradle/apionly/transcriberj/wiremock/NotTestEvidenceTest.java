package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import com.arc_e_tect.gradle.doppelganger.detect.VerifiedContractTest;
import com.arc_e_tect.gradle.detector.core.scan.PropertyResolutionContext;
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
 * T21.14: stubs are not test evidence. The contract-evidence scan the API-Only Suite runs over test
 * sources -- the Doppelganger API Detector's, reading them without a classpath -- finds no test in
 * the {@code java} format's class, nor in the mapping files; and over the REST Docs emitter's tests
 * with the stubs of both formats beside them, exactly what it finds in the tests alone.
 */
@DisplayName("T21.14 Stubs are not test evidence")
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
        Path java = suite.generated.java();
        Path files = suite.files();
        Path restdocs = suite.generated.restdocs();
        Path together = directory.resolve(contract.name());
        copy(java, together.resolve("java"));
        copy(files, together.resolve("files"));
        copy(restdocs, together.resolve("restdocs"));

        List<VerifiedContractTest> testsAlone = scanner.scanWithStatusCodes(restdocs.toFile());
        assertThat(scanner.scanWithStatusCodes(java.toFile())).isEmpty();
        assertThat(scanner.scanWithStatusCodes(files.toFile())).isEmpty();
        assertThat(testsAlone).hasSize(suite.cases.size());
        List<VerifiedContractTest> all = scanner.scanWithStatusCodes(together.toFile());
        assertThat(all.stream().map(NotTestEvidenceTest::key).sorted().toList())
                .isEqualTo(testsAlone.stream().map(NotTestEvidenceTest::key).sorted().toList());
    }

    private static String key(VerifiedContractTest test) {
        return test.endpoint().verb() + " " + test.endpoint().path() + " " + test.statusCode();
    }

    private static Map<String, String> index(GeneratedSuite suite) throws IOException {
        Properties index = new Properties();
        Path file = suite.generated.core().getParent().resolve("contract-endpoints.properties");
        try (Reader in = Files.newBufferedReader(file, StandardCharsets.ISO_8859_1)) {
            index.load(in);
        }
        Map<String, String> properties = new TreeMap<>();
        index.forEach((k, v) -> properties.put((String) k, (String) v));
        return properties;
    }

    private static void copy(Path from, Path to) throws IOException {
        try (Stream<Path> walk = Files.walk(from)) {
            for (Path f : walk.toList()) {
                Path target = to.resolve(from.relativize(f).toString());
                if (Files.isDirectory(f)) Files.createDirectories(target);
                else Files.copy(f, target);
            }
        }
    }
}
