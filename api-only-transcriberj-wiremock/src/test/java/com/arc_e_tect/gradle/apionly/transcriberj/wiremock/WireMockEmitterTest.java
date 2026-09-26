package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import com.arc_e_tect.gradle.apionly.transcriberj.spi.Emitter;
import com.arc_e_tect.gradle.apionly.transcriberj.spi.ManagedDependency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The emitter as the TranscriberJ loads it: its id, the dependency it manages (T15.15), its one
 * option, and a response body the core cannot build.
 */
@DisplayName("The WireMock emitter")
class WireMockEmitterTest {

    @TempDir
    Path directory;

    @Test
    void theEmitterDescribesItselfAndIsAService() {
        WireMockEmitter emitter = new WireMockEmitter();
        assertThat(emitter.id()).isEqualTo("wiremock");
        assertThat(emitter.represents()).isEmpty();
        assertThat(ServiceLoader.load(Emitter.class)).extracting(Emitter::id).contains("wiremock");
    }

    /** T15.15: the artifact wiremock-spring-boot 4.x brings, at the version it brings, untested from 4. */
    @Test
    void itManagesTheWireMockThatWireMockSpringBootBrings() {
        assertThat(new WireMockEmitter().dependencies())
                .containsExactly(new ManagedDependency("org.wiremock", "wiremock-jetty12", "3.13.2", "4"));
    }

    /** T15.15: a project that resolves a version outside the tested range gets the TranscriberJ's warning. */
    @Test
    void anUntestedVersionGetsTheTranscriberJsWarning() throws Exception {
        Class<?> managed = Class.forName("com.arc_e_tect.gradle.apionly.transcriberj.ManagedDependencies");
        Method check = managed.getDeclaredMethod("check", String.class, Map.class, Map.class, boolean.class,
                Consumer.class);
        check.setAccessible(true);
        Map<String, List<ManagedDependency>> byEmitter = Map.of("wiremock", new WireMockEmitter().dependencies());

        for (String tested : List.of("3.13.2", "3.99.0")) {
            List<String> warnings = new ArrayList<>();
            check.invoke(null, "testCompileClasspath", Map.of("org.wiremock:wiremock-jetty12", tested), byEmitter,
                    false, (Consumer<String>) warnings::add);
            assertThat(warnings).as(tested).isEmpty();
        }
        for (String untested : List.of("4.0.0", "4.0.0-beta.38")) {
            List<String> warnings = new ArrayList<>();
            check.invoke(null, "testCompileClasspath", Map.of("org.wiremock:wiremock-jetty12", untested), byEmitter,
                    false, (Consumer<String>) warnings::add);
            assertThat(warnings).as(untested).containsExactly("Emitter wiremock: org.wiremock:wiremock-jetty12 "
                    + untested + " in testCompileClasspath is not tested with this plugin version; it is tested with "
                    + "versions below 4, and adds 3.13.2 when the project declares none.");
        }
    }

    @Test
    void thePriorityIsAPositiveWholeNumber() {
        assertThat(WireMockEmitter.priority(Map.of())).isEqualTo(1);
        assertThat(WireMockEmitter.priority(Map.of("priority", "12"))).isEqualTo(12);
        for (String wrong : List.of("0", "-1", "high", "1.5", "")) {
            assertThatThrownBy(() -> WireMockEmitter.priority(Map.of("priority", wrong)))
                    .as(wrong).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("The WireMock emitter's option 'priority' is '" + wrong + "'; it must be a whole "
                            + "number of 1 or more, WireMock's highest priority being 1.");
        }
    }

    @Test
    void anOptionItDoesNotHaveFailsGeneration() {
        assertThatThrownBy(() -> Fixtures.generate(Fixtures.RESPONSES, Map.of("priorty", "2"), directory))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The WireMock emitter has no option 'priorty'; its only option is 'priority'.");
    }

    /**
     * A case whose response body the core cannot build still gets its mapping generated -- the core
     * does not tell an emitter which bodies it degrades -- and registering it fails with the core's
     * own reason, as calling any degraded method does.
     */
    @Test
    void aResponseBodyTheCoreCannotBuildFailsWhenItsMappingIsMade() throws Exception {
        Path contract = directory.resolve("openapi.yaml");
        Files.writeString(contract, """
                openapi: 3.1.0
                info:
                  title: Degraded response
                  version: 1.0.0
                paths:
                  /odd:
                    get:
                      operationId: getOdd
                      parameters:
                        - {name: page, in: query, required: true, schema: {type: integer, minimum: 1}}
                      responses:
                        '200':
                          description: Fine.
                        '400':
                          description: Invalid.
                          content:
                            application/problem+json:
                              schema:
                                $ref: '#/components/schemas/OddProblemV1'
                components:
                  schemas:
                    OddProblemV1:
                      x-fragment-path: schemas/OddProblemV1.yaml
                      type: object
                      required: [detail]
                      properties:
                        detail:
                          oneOf:
                            - {type: string}
                            - {type: integer}
                """);
        Fixtures.Contract odd = new Fixtures.Contract("odd", null, List.of());
        GeneratedSuite suite = GeneratedSuite.of(odd, contract, null, directory.resolve("odd"));

        assertThat(suite.cases).isNotEmpty();
        assertThatThrownBy(() -> suite.mapping(suite.cases.get(0)))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("OddProblemV1");
    }
}
