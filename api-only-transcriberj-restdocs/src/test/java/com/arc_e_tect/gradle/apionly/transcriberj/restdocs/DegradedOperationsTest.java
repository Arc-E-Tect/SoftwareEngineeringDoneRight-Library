package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T14.12 and T20.10: an operation whose body has an inline {@code oneOf} gets tests for its other
 * cases, and none for the degraded construct: no full success test, since its full request reaches
 * the construct, but its required success test and its invalid-request tests, which pass against the
 * validating server. An operation with no valid request has no case of any kind -- every kind is
 * built from the required request -- and gets no interface; nor does one that declares no response
 * a case can be derived for.
 */
@DisplayName("T14.12, T20.10 Degraded and uncovered operations")
class DegradedOperationsTest {

    @Test
    void theOtherCasesAreRenderedAndPass() {
        GeneratedSuite suite = Suites.of(Fixtures.DEGRADED);

        assertThat(suite.cases("INVALID_REQUEST")).isNotEmpty();
        assertThat(suite.cases("INVALID_REQUEST")).extracting(c -> c.json().get("pointer").stringValue(null))
                .noneMatch(p -> p != null && p.startsWith("/shape"));
        assertThat(suite.cases("INVALID_REQUEST")).extracting(c -> c.json().get("pointer").stringValue(null))
                .contains("/name");
        assertThat(suite.report.get("constraintCoverage").toString()).contains("inside a degraded construct");
        GeneratedSuite.Run run;
        try (ContractServer server = new ContractServer(ValidatingServer.of(suite))) {
            run = suite.run(server);
        }
        assertThat(run.failed()).isEmpty();
        assertThat(run.outcomes()).hasSize(suite.cases.size());
    }

    @Test
    void aSuccessWhoseFullRequestIsDegradedHasItsRequiredTestAndItsOtherTests(@TempDir Path directory)
            throws Exception {
        Path contract = directory.resolve("openapi.yaml");
        Files.writeString(contract, Files.readString(Fixtures.DEGRADED.document()).replace(
                "      responses:\n        '400':",
                "      responses:\n        '201':\n          description: Created.\n        '400':"));
        GeneratedSuite suite = GeneratedSuite.of("degraded-success", contract, directory.resolve("suite"));

        assertThat(suite.cases("SUCCESS")).extracting(GeneratedSuite.Case::id).containsExactly("success-201-required");
        assertThat(suite.cases("INVALID_REQUEST")).isNotEmpty();
        assertThat(suite.generated.report().notes()).anyMatch(n -> n.contains("success-201-full is not derived"));
        GeneratedSuite.Run run;
        try (ContractServer server = new ContractServer(ValidatingServer.of(suite))) {
            run = suite.run(server);
        }
        assertThat(run.failed()).isEmpty();
        assertThat(run.outcomes()).hasSize(suite.cases.size());
    }

    @Test
    void anOperationWithNoDerivableCaseGetsNoInterface(@TempDir Path directory) throws Exception {
        Path contract = directory.resolve("openapi.yaml");
        Files.writeString(contract, """
                openapi: 3.1.0
                info: {title: T, version: 1.0.0}
                paths:
                  /impossible:
                    post:
                      operationId: postImpossible
                      requestBody:
                        required: true
                        content:
                          application/json:
                            schema: {$ref: '#/components/schemas/ImpossibleV1'}
                      responses:
                        '201': {description: Never.}
                        '400': {description: Invalid.}
                  /failing:
                    get:
                      operationId: getFailing
                      responses:
                        '503': {description: Unavailable.}
                  /fine:
                    get:
                      operationId: getFine
                      responses:
                        '200': {description: Fine.}
                components:
                  schemas:
                    ImpossibleV1:
                      x-fragment-path: schemas/ImpossibleV1.yaml
                      type: object
                      required: [code]
                      properties:
                        code: {type: string, minLength: 5, maxLength: 2}
                """);
        Fixtures.Generated generated = Fixtures.generate(contract, new Fixtures.Contract("uncovered", null, List.of())
                .settings(Map.of("tests", "true")), directory, List.of(new RestDocsEmitter()));
        Path restdocs = generated.sources().resolve("com/example/contract/restdocs");

        assertThat(restdocs.resolve("GetFineContractTests.java")).exists();
        assertThat(restdocs.resolve("PostImpossibleContractTests.java")).doesNotExist();
        assertThat(restdocs.resolve("GetFailingContractTests.java")).doesNotExist();
    }
}
