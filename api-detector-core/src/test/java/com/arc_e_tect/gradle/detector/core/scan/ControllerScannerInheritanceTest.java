package com.arc_e_tect.gradle.detector.core.scan;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Unit tests for {@link ControllerScanner#scan(java.util.Collection)}: endpoints a
 * {@code @RestController} inherits from the interfaces it implements and the classes it extends,
 * as Spring MVC serves them.
 *
 * <p>The fixtures under {@code src/test/resources/fixtures/inherited/} are scanned together, the
 * way a detector scans a whole source tree.</p>
 */
@DisplayName("ControllerScanner, inherited mappings")
class ControllerScannerInheritanceTest {

    @Test
    @DisplayName("includes the mappings of an implemented interface, with its path constants and an empty base-path placeholder")
    void includesImplementedInterfaceMappings() throws Exception {
        List<Endpoint> endpoints = new ControllerScanner().scan(inheritedFixtures());

        assertThat(endpoints)
                .filteredOn(e -> e.declaringClass().equals("com.example.inherited.web.BookController"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactlyInAnyOrder(
                        tuple(HttpVerb.GET, "/v1/books/{id}"),
                        tuple(HttpVerb.POST, "/v1/books"),
                        tuple(HttpVerb.GET, "/v1/books"));
    }

    @Test
    @DisplayName("reports an overridden inherited method at the controller's own method")
    void reportsOverriddenMethodAtController() throws Exception {
        List<Endpoint> endpoints = new ControllerScanner().scan(inheritedFixtures());

        assertThat(endpoints)
                .filteredOn(e -> e.declaringClass().equals("com.example.inherited.web.BookController"))
                .filteredOn(e -> e.methodSignature().startsWith("getBook"))
                .extracting(Endpoint::methodSignature, Endpoint::sourceFile, Endpoint::lineNumber)
                .containsExactly(tuple("getBook(String)", "BookController.java", 10));
    }

    @Test
    @DisplayName("reports an inherited default method that is not overridden at the method in its supertype")
    void reportsNonOverriddenMethodAtSupertype() throws Exception {
        List<Endpoint> endpoints = new ControllerScanner().scan(inheritedFixtures());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("listBooks"))
                .extracting(Endpoint::declaringClass, Endpoint::methodSignature, Endpoint::sourceFile, Endpoint::lineNumber)
                .containsExactly(tuple("com.example.inherited.web.BookController", "listBooks()", "BooksApi.java", 24));
    }

    @Test
    @DisplayName("never reports an interface's mappings on their own, nor those of an interface with the same simple name in another package")
    void ignoresInterfacesThemselvesAndSameNamedOtherInterface() throws Exception {
        List<Endpoint> endpoints = new ControllerScanner().scan(inheritedFixtures());

        assertThat(endpoints).extracting(Endpoint::path).doesNotContain("/wrong", "/summary");
        assertThat(endpoints).extracting(Endpoint::declaringClass)
                .allMatch(declaringClass -> declaringClass.endsWith("Controller"));
    }

    @Test
    @DisplayName("includes the mappings and the base path of an extended superclass")
    void includesSuperclassMappings() throws Exception {
        List<Endpoint> endpoints = new ControllerScanner().scan(inheritedFixtures());

        assertThat(endpoints)
                .filteredOn(e -> e.declaringClass().equals("com.example.inherited.web.AuthorController"))
                .extracting(Endpoint::verb, Endpoint::path, Endpoint::sourceFile)
                .containsExactlyInAnyOrder(
                        tuple(HttpVerb.GET, "/v1/authors/{id}", "AuthorController.java"),
                        tuple(HttpVerb.DELETE, "/v1/authors/{id}", "AbstractAuthorController.java"));
    }

    @Test
    @DisplayName("reports no endpoint for a method that neither the controller nor its supertype maps")
    void ignoresUnmappedMethodInBothControllerAndSupertype() throws Exception {
        List<Endpoint> endpoints = new ControllerScanner().scan(inheritedFixtures());

        assertThat(endpoints).noneMatch(e -> e.methodSignature().startsWith("describe"));
    }

    @Test
    @DisplayName("resolves a sibling nested supertype referenced by its simple name")
    void resolvesSiblingNestedSupertype() throws Exception {
        List<Endpoint> endpoints = new ControllerScanner().scan(inheritedFixtures());

        assertThat(endpoints)
                .filteredOn(e -> e.declaringClass().equals("com.example.inherited.web.SiblingControllers.SiblingController"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.GET, "/v1/siblings"));
    }

    @Test
    @DisplayName("resolves a supertype in the default package")
    void resolvesSupertypeInDefaultPackage() throws Exception {
        List<Endpoint> endpoints = new ControllerScanner().scan(inheritedFixtures());

        assertThat(endpoints)
                .filteredOn(e -> e.declaringClass().equals("DefaultPackageController"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.GET, "/v1/default-package"));
    }

    @Test
    @DisplayName("lets the controller's own mappings win over inherited ones, follows super-interfaces and a fully qualified supertype, and skips a supertype that was not scanned")
    void ownMappingsWinAndSuperInterfacesAreFollowed() throws Exception {
        List<Endpoint> endpoints = new ControllerScanner().scan(inheritedFixtures());

        assertThat(endpoints)
                .filteredOn(e -> e.declaringClass().equals("com.example.inherited.web.StatusController"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactlyInAnyOrder(
                        tuple(HttpVerb.GET, "/status/ping"),
                        tuple(HttpVerb.GET, "/status/details"));
    }

    @Test
    @DisplayName("resolves a generic supertype through a wildcard import")
    void resolvesGenericSupertypeThroughWildcardImport() throws Exception {
        List<Endpoint> endpoints = new ControllerScanner().scan(inheritedFixtures());

        assertThat(endpoints)
                .filteredOn(e -> e.declaringClass().equals("com.example.inherited.web.ShelfController"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.GET, "/v1/shelves/{id}"));
    }

    @Test
    @DisplayName("resolves a nested supertype referenced through its enclosing type")
    void resolvesNestedSupertype() throws Exception {
        List<Endpoint> endpoints = new ControllerScanner().scan(inheritedFixtures());

        assertThat(endpoints)
                .filteredOn(e -> e.declaringClass().equals("com.example.inherited.web.HealthController"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.GET, "/health"));
    }

    @Test
    @DisplayName("visits each supertype once, even in a cyclic hierarchy")
    void visitsCyclicHierarchyOnce() throws Exception {
        List<Endpoint> endpoints = new ControllerScanner().scan(inheritedFixtures());

        assertThat(endpoints)
                .filteredOn(e -> e.declaringClass().equals("com.example.inherited.web.LoopController"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.GET, "/v1/loops"));
    }

    @Test
    @DisplayName("resolves another scanned class's path constant")
    void resolvesAnotherScannedClassConstant() throws Exception {
        List<Endpoint> endpoints = new ControllerScanner().scan(inheritedFixtures());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("listReviews"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.GET, "/v1/reviews"));
    }

    @Test
    @DisplayName("resolves a placeholder to its default when no property is given")
    void resolvesPlaceholderToItsDefault() throws Exception {
        List<Endpoint> endpoints = new ControllerScanner().scan(inheritedFixtures());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("getReview"))
                .extracting(Endpoint::path)
                .containsExactly("/items/{id}");
    }

    @Test
    @DisplayName("resolves placeholders to the given properties")
    void resolvesPlaceholdersToGivenProperties() throws Exception {
        PropertyResolutionContext context = PropertyResolutionContext.of(
                Map.of("openapi.books.base-path", "/api",
                        "reviews.base-path", "/r",
                        "reviews.item-path", "/entries"),
                Set.of());

        List<Endpoint> endpoints = new ControllerScanner(context).scan(inheritedFixtures());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("getBook")
                        || e.methodSignature().startsWith("listReviews")
                        || e.methodSignature().startsWith("getReview"))
                .extracting(Endpoint::path)
                .containsExactlyInAnyOrder("/api/v1/books/{id}", "/r/v1/reviews", "/r/entries/{id}");
    }

    @Test
    @DisplayName("lets a given property win over a scanned constant with the same key")
    void givenPropertyWinsOverScannedConstant() throws Exception {
        PropertyResolutionContext context = PropertyResolutionContext.of(Map.of("Paths.REVIEWS", "/v2/reviews"), Set.of());

        List<Endpoint> endpoints = new ControllerScanner(context).scan(inheritedFixtures());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("listReviews"))
                .extracting(Endpoint::path)
                .containsExactly("/v2/reviews");
    }

    @Test
    @DisplayName("lets a placeholder without a property and without a default contribute no path")
    void unresolvablePlaceholderContributesNoPath() throws Exception {
        List<Endpoint> endpoints = new ControllerScanner().scan(inheritedFixtures());

        assertThat(endpoints).extracting(Endpoint::path).noneMatch(path -> path.contains("${"));
        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("listReviews"))
                .extracting(Endpoint::path)
                .containsExactly("/v1/reviews");
    }

    @Test
    @DisplayName("skips a file that is not valid Java and still scans the others")
    void skipsUnparsableFile(@TempDir Path tempDir) throws Exception {
        File garbage = new File(tempDir.toFile(), "NotJava.java");
        Files.writeString(garbage.toPath(), "this is not java { {{ }}}}}");
        List<File> files = new ArrayList<>(inheritedFixtures());
        files.add(0, garbage);

        List<Endpoint> endpoints = new ControllerScanner().scan(files);

        assertThat(endpoints).extracting(Endpoint::path).contains("/v1/books/{id}");
    }

    @Test
    @DisplayName("finds nothing inherited when the supertype's file is not among the scanned files")
    void findsNothingWhenSupertypeIsNotScanned() throws Exception {
        File controller = inheritedFixtures().stream()
                .filter(f -> f.getName().equals("BookController.java"))
                .findFirst().orElseThrow();

        assertThat(new ControllerScanner().scan(controller)).isEmpty();
    }

    private static List<File> inheritedFixtures() throws IOException, URISyntaxException {
        URL url = ControllerScannerInheritanceTest.class.getClassLoader().getResource("fixtures/inherited");
        if (url == null) {
            throw new IllegalStateException("Fixture directory not found on classpath: fixtures/inherited");
        }
        try (Stream<Path> paths = Files.walk(Path.of(url.toURI()))) {
            return paths.filter(p -> p.toString().endsWith(".java")).sorted().map(Path::toFile).toList();
        }
    }
}
