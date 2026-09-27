package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import com.arc_e_tect.gradle.detector.core.model.PathTemplates;
import com.arc_e_tect.gradle.detector.core.scan.PropertyResolutionContext;
import com.arc_e_tect.gradle.doppelganger.detect.VerifiedContractTest;
import com.arc_e_tect.gradle.doppelganger.scan.RestDocsScanner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T14.10 and T20.8: the Doppelganger API Detector's contract-evidence scan -- the one the API-Only
 * Suite runs, reading test sources without a classpath and resolving {@code ClassName.PATH}
 * through the TranscriberJ's endpoint index -- counts every operation with cases as having a
 * conformance test, and every generated test, of every kind, as one.
 */
@DisplayName("T14.10, T20.8 Doppelganger visibility")
class DoppelgangerVisibilityTest {

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void everyOperationWithCasesHasAConformanceTest(Fixtures.Contract contract) throws IOException {
        GeneratedSuite suite = Suites.of(contract);
        Properties index = new Properties();
        try (Reader in = Files.newBufferedReader(suite.generated.index(), java.nio.charset.StandardCharsets.ISO_8859_1)) {
            index.load(in);
        }
        Map<String, String> properties = new TreeMap<>();
        index.forEach((k, v) -> properties.put((String) k, (String) v));

        List<VerifiedContractTest> found = new RestDocsScanner("", PropertyResolutionContext.of(properties, Set.of()))
                .scanWithStatusCodes(suite.source("com/example/contract/restdocs").toFile());

        assertThat(found).hasSize(suite.cases.size());
        for (JsonNode operation : suite.report.get("contractCases")) {
            if (operation.get("cases").isEmpty()) continue;
            String verb = operation.get("method").stringValue();
            String path = PathTemplates.normalize(operation.get("pathTemplate").stringValue());
            assertThat(found).as("%s %s", verb, path).filteredOn(t -> t.endpoint().verb().name().equals(verb)
                    && t.endpoint().path().equals(path)).hasSize(operation.get("cases").size());
        }
        // The scan reads a status only from a literal status assertion; the generated tests take theirs
        // from the case, so it counts every test as evidence for its operation and none for a status.
        assertThat(found).extracting(VerifiedContractTest::statusCode).containsOnlyNulls();
    }
}
