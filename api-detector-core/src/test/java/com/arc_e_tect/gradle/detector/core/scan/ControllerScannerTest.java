package com.arc_e_tect.gradle.detector.core.scan;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Unit tests for {@link ControllerScanner}.
 *
 * <p>Uses fixture {@code .java} source files placed under
 * {@code src/test/resources/fixtures/} so JavaParser can parse them as text - Spring itself does
 * not need to be on the test classpath.</p>
 */
@DisplayName("ControllerScanner")
class ControllerScannerTest {

    private final ControllerScanner scanner = new ControllerScanner();

    @Test
    @DisplayName("ignores classes annotated with @Controller instead of @RestController")
    void ignoresPlainController() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixture("PlainControllerFixture.java"));

        assertThat(endpoints).isEmpty();
    }

    @Test
    @DisplayName("combines the class-level base path with a no-argument @GetMapping")
    void combinesBasePathWithNoArgGetMapping() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixture("SimpleControllerFixture.java"));

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("listUsers"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.GET, "/api/users"));
    }

    @Test
    @DisplayName("combines the class-level base path with a path-argument mapping")
    void combinesBasePathWithPathArgument() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixture("SimpleControllerFixture.java"));

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("getUser"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.GET, "/api/users/{id}"));
    }

    @Test
    @DisplayName("strips regex constraints from path variables")
    void stripsRegexConstraints() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixture("SimpleControllerFixture.java"));

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("createUser"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.POST, "/api/users/{id}"));
    }

    @Test
    @DisplayName("resolves the path attribute given as path= instead of value=")
    void resolvesPathAttribute() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixture("SimpleControllerFixture.java"));

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("replaceUser"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.PUT, "/api/users/{id}"));
    }

    @Test
    @DisplayName("resolves @PatchMapping and @DeleteMapping shortcuts")
    void resolvesRemainingShortcutAnnotations() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixture("SimpleControllerFixture.java"));

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("patchUser"))
                .extracting(Endpoint::verb)
                .containsExactly(HttpVerb.PATCH);
        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("deleteUser"))
                .extracting(Endpoint::verb)
                .containsExactly(HttpVerb.DELETE);
    }

    @Test
    @DisplayName("resolves @RequestMapping with an explicit single method")
    void resolvesRequestMappingWithSingleMethod() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixture("SimpleControllerFixture.java"));

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("archiveUser"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.POST, "/api/users/{id}/archive"));
    }

    @Test
    @DisplayName("expands @RequestMapping with multiple paths and multiple methods into every combination")
    void expandsMultiplePathsAndMethods() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixture("SimpleControllerFixture.java"));

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("userTags"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactlyInAnyOrder(
                        tuple(HttpVerb.GET, "/api/users/{id}/tags"),
                        tuple(HttpVerb.HEAD, "/api/users/{id}/tags"),
                        tuple(HttpVerb.GET, "/api/users/{id}/labels"),
                        tuple(HttpVerb.HEAD, "/api/users/{id}/labels"));
    }

    @Test
    @DisplayName("uses HttpVerb.ANY for a @RequestMapping without a restricted method")
    void usesAnyVerbForUnrestrictedRequestMapping() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixture("SimpleControllerFixture.java"));

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("userSummary"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.ANY, "/api/users/{id}/summary"));
    }

    @Test
    @DisplayName("ignores methods without a mapping annotation")
    void ignoresMethodsWithoutMappingAnnotation() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixture("SimpleControllerFixture.java"));

        assertThat(endpoints).noneMatch(e -> e.methodSignature().startsWith("notAnEndpoint"));
    }

    @Test
    @DisplayName("defaults to the root path when there is no class-level @RequestMapping")
    void defaultsToRootPathWithoutClassMapping() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixture("NoBasePathControllerFixture.java"));

        assertThat(endpoints)
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.GET, "/health"));
    }

    @Test
    @DisplayName("scans a @RestController nested inside another class")
    void scansNestedController() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixture("NestedControllerFixture.java"));

        assertThat(endpoints)
                .extracting(Endpoint::declaringClass, Endpoint::verb, Endpoint::path)
                .containsExactly(tuple("com.example.fixture.NestedControllerFixture.Inner",
                        HttpVerb.GET, "/nested/ping"));
    }

    @Test
    @DisplayName("sets the source file name and a positive line number on every endpoint")
    void setsSourceFileNameAndLineNumber() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixture("SimpleControllerFixture.java"));

        assertThat(endpoints).isNotEmpty();
        assertThat(endpoints)
                .allSatisfy(e -> {
                    assertThat(e.sourceFile()).isEqualTo("SimpleControllerFixture.java");
                    assertThat(e.lineNumber()).isPositive();
                    assertThat(e.declaringClass()).isEqualTo("com.example.fixture.SimpleControllerFixture");
                });
    }

    @Test
    @DisplayName("returns empty list when the file is not valid Java")
    void returnsEmptyListForUnparsableFile(@TempDir Path tempDir) throws Exception {
        File garbage = new File(tempDir.toFile(), "NotJava.java");
        Files.writeString(garbage.toPath(), "this is not java { {{ }}}}}");

        assertThat(scanner.scan(garbage)).isEmpty();
    }

    private static File fixture(String name) {
        URL url = ControllerScannerTest.class.getClassLoader().getResource("fixtures/" + name);
        if (url == null) {
            throw new IllegalStateException("Fixture not found on classpath: fixtures/" + name);
        }
        return new File(url.getFile());
    }
}
