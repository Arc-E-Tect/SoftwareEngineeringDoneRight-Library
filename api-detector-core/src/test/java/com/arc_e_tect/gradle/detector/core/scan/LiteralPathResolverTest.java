package com.arc_e_tect.gradle.detector.core.scan;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LiteralPathResolver}, isolated from any one scanner that happens to call
 * it - {@link ControllerScannerTest} and its counterparts in {@code doppelganger-api-detector}
 * additionally exercise it indirectly through their own fixtures.
 */
@DisplayName("LiteralPathResolver")
class LiteralPathResolverTest {

    @Test
    @DisplayName("resolves a string literal directly")
    void resolvesLiteral() {
        assertThat(resolve("literalArgument", "get")).contains("/v1/health");
    }

    @Test
    @DisplayName("resolves a name that refers to a field constant declared elsewhere in the same file")
    void resolvesFieldConstant() {
        assertThat(resolve("fieldConstantArgument", "get")).contains("/v1/users/{username}");
    }

    @Test
    @DisplayName("resolves a name that refers to a local variable declared earlier in the same method")
    void resolvesLocalVariable() {
        assertThat(resolve("localVariableArgument", "post")).contains("/v1/users/registrations");
    }

    @Test
    @DisplayName("does not resolve a value computed by a method call")
    void doesNotResolveMethodCall() {
        assertThat(resolve("methodCallArgument", "delete")).isEmpty();
    }

    @Test
    @DisplayName("without a PropertyResolutionContext, does not resolve a configured helper-method call")
    void doesNotResolveHelperMethodWithoutContext() {
        assertThat(resolve("helperMethodArgument", "post")).isEmpty();
    }

    @Test
    @DisplayName("resolves a configured helper-method call against the merged property map")
    void resolvesHelperMethodCall() {
        PropertyResolutionContext context = PropertyResolutionContext.of(
                Map.of("users.registrations", "/v1/users/registrations"),
                Set.of("ApiEndpoints.get"));
        assertThat(resolve("helperMethodArgument", "post", context)).contains("/v1/users/registrations");
    }

    @Test
    @DisplayName("does not resolve a helper-method call whose key is absent from the property map")
    void doesNotResolveHelperMethodWithUnknownKey() {
        PropertyResolutionContext context = PropertyResolutionContext.of(
                Map.of("users.registrations", "/v1/users/registrations"),
                Set.of("ApiEndpoints.get"));
        assertThat(resolve("helperMethodUnknownKeyArgument", "post", context)).isEmpty();
    }

    @Test
    @DisplayName("does not resolve a method call that is not a configured helper-method convention")
    void doesNotResolveUnconfiguredHelperMethod() {
        PropertyResolutionContext context = PropertyResolutionContext.of(
                Map.of("users.registrations", "/v1/users/registrations"),
                Set.of("SomeOtherClass.get"));
        assertThat(resolve("helperMethodArgument", "post", context)).isEmpty();
    }

    @Test
    @DisplayName("without a PropertyResolutionContext, does not resolve a field constant initialized by a helper-method call")
    void doesNotResolveHelperMethodFieldConstantWithoutContext() {
        assertThat(resolve("helperMethodFieldConstantArgument", "get")).isEmpty();
    }

    @Test
    @DisplayName("resolves a field constant initialized by a configured helper-method call against the merged property map")
    void resolvesHelperMethodFieldConstant() {
        PropertyResolutionContext context = PropertyResolutionContext.of(
                Map.of("users.registrations.completion", "/v1/users/registrations/completion/{verificationLink}"),
                Set.of("ApiEndpoints.get"));
        assertThat(resolve("helperMethodFieldConstantArgument", "get", context))
                .contains("/v1/users/registrations/completion/{verificationLink}");
    }

    @Test
    @DisplayName("without a PropertyResolutionContext, does not resolve a @Value-annotated field")
    void doesNotResolveValueAnnotationWithoutContext() {
        assertThat(resolve("valueAnnotationArgument", "get")).isEmpty();
    }

    @Test
    @DisplayName("resolves a @Value-annotated field against the merged property map")
    void resolvesValueAnnotation() {
        PropertyResolutionContext context = PropertyResolutionContext.of(
                Map.of("users.by-username", "/v1/users/{username}"), Set.of());
        assertThat(resolve("valueAnnotationArgument", "get", context)).contains("/v1/users/{username}");
    }

    @Test
    @DisplayName("falls back to the @Value default when the key is absent from the property map")
    void resolvesValueAnnotationDefaultFallback() {
        PropertyResolutionContext context = PropertyResolutionContext.of(Map.of(), Set.of());
        assertThat(resolve("valueAnnotationWithDefaultArgument", "get", context)).contains("/v1/fallback");
    }

    @Test
    @DisplayName("does not resolve a @Value-annotated field with no default whose key is absent")
    void doesNotResolveValueAnnotationWithoutDefaultOrKey() {
        PropertyResolutionContext context = PropertyResolutionContext.of(Map.of(), Set.of());
        assertThat(resolve("valueAnnotationUnknownKeyNoDefaultArgument", "get", context)).isEmpty();
    }

    /**
     * Parses the fixture, finds the named default method, and resolves the first argument of the
     * call to {@code verbMethodName} within it (e.g. {@code get}/{@code post}/{@code delete}),
     * without any {@link PropertyResolutionContext}.
     */
    private Optional<String> resolve(String methodName, String verbMethodName) {
        return resolve(methodName, verbMethodName, PropertyResolutionContext.empty());
    }

    /**
     * As {@link #resolve(String, String)}, but resolving with the given
     * {@link PropertyResolutionContext}.
     */
    private Optional<String> resolve(String methodName, String verbMethodName, PropertyResolutionContext context) {
        CompilationUnit cu = parseFixture();
        MethodDeclaration method = cu.findAll(MethodDeclaration.class).stream()
                .filter(m -> m.getNameAsString().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No such method in fixture: " + methodName));
        MethodCallExpr call = method.findAll(MethodCallExpr.class).stream()
                .filter(c -> c.getNameAsString().equals(verbMethodName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No such call in " + methodName + ": " + verbMethodName));
        Expression argument = call.getArgument(0);
        return LiteralPathResolver.resolve(argument, context);
    }

    private CompilationUnit parseFixture() {
        URL url = LiteralPathResolverTest.class.getClassLoader()
                .getResource("fixtures/scan/LiteralPathResolverFixture.java");
        if (url == null) {
            throw new IllegalStateException("Fixture not found on classpath: fixtures/scan/LiteralPathResolverFixture.java");
        }
        try {
            return new JavaParser().parse(new File(url.getFile())).getResult()
                    .orElseThrow(() -> new IllegalStateException("Fixture did not parse"));
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Fixture not found: " + url, e);
        }
    }
}
