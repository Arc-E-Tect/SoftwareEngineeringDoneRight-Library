package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import com.arc_e_tect.gradle.apionly.transcriberj.core.Generation;
import com.arc_e_tect.gradle.apionly.transcriberj.core.GenerationReport;
import com.arc_e_tect.gradle.apionly.transcriberj.model.Construct;
import com.arc_e_tect.gradle.apionly.transcriberj.spi.Emitter;
import com.arc_e_tect.gradle.apionly.transcriberj.spi.ManagedDependency;
import com.arc_e_tect.gradle.apionly.transcriberj.spi.Settings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.payload.RequestFieldsSnippet;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.restdocs.payload.SubsectionDescriptor;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** The emitter, run over real contracts by the TranscriberJ, and its output compiled and called. */
class RestDocsEmitterTest {

    private static final String PACKAGE = "com.example.contract";

    @TempDir
    Path directory;

    private GenerationReport report;

    private ClassLoader generate(Path contract, String version) throws Exception {
        Path sources = directory.resolve("sources");
        report = Generation.run(contract, version, "x",
                new Settings("test", PACKAGE, false, "PLACEHOLDER", 2), sources, List.of(new RestDocsEmitter()));
        Path classes = Files.createDirectories(directory.resolve("classes"));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        List<Path> files;
        try (Stream<Path> walk = Files.walk(sources)) {
            files = walk.filter(p -> p.toString().endsWith(".java")).toList();
        }
        try (StandardJavaFileManager fileManager =
                     compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            boolean ok = compiler.getTask(null, fileManager, diagnostics,
                    List.of("-d", classes.toString(), "-classpath", System.getProperty("java.class.path"),
                            "-Xlint:all", "-Xdoclint:all,-missing", "--release", "21"),
                    null, fileManager.getJavaFileObjectsFromPaths(files)).call();
            assertThat(diagnostics.getDiagnostics().stream()
                    .filter(d -> d.getKind() != Diagnostic.Kind.NOTE).map(Object::toString)).isEmpty();
            assertThat(ok).isTrue();
        }
        return new URLClassLoader(new URL[]{classes.toUri().toURL()}, getClass().getClassLoader());
    }

    private ClassLoader userAccount() throws Exception {
        return generate(Path.of(getClass().getResource("/contracts/user-account/openapi.yaml").toURI()), "1.0.0");
    }

    private static Object call(ClassLoader loader, String className, String method, Object... args)
            throws Exception {
        Class<?> type = Class.forName(PACKAGE + ".restdocs." + className, true, loader);
        Method m = Arrays.stream(type.getMethods()).filter(x -> x.getName().equals(method)
                && x.getParameterCount() == args.length).findFirst().orElseThrow();
        return m.invoke(null, args);
    }

    private static List<String> describe(Object descriptors) {
        return Arrays.stream((FieldDescriptor[]) descriptors).map(d -> d.getPath() + " " + d.getType()
                + (d.isOptional() ? " optional" : "") + (d instanceof SubsectionDescriptor ? " subsection" : "")
                + " " + d.getDescription()).toList();
    }

    private static boolean relaxed(Object snippet) throws Exception {
        Method ignored = org.springframework.restdocs.payload.AbstractFieldsSnippet.class
                .getDeclaredMethod("isIgnoredUndocumentedFields");
        ignored.setAccessible(true);
        return (boolean) ignored.invoke(snippet);
    }

    @Test
    void theEmitterDescribesItselfAndIsAService() {
        RestDocsEmitter emitter = new RestDocsEmitter();
        assertThat(emitter.id()).isEqualTo("restdocs");
        assertThat(emitter.dependencies()).containsExactly(new ManagedDependency(
                "org.springframework.restdocs", "spring-restdocs-core", "4.0.1", "5"));
        assertThat(emitter.represents()).contains(Construct.ADDITIONAL_PROPERTIES, Construct.RECURSIVE_REF)
                .doesNotContain(Construct.MULTIPLE_TYPES, Construct.ONE_OF_INLINE_BRANCHES);
        assertThat(ServiceLoader.load(Emitter.class)).extracting(Emitter::id).contains("restdocs");
    }

    @Test
    void theUserAccountCompanionsDescribeWhatTheHandWrittenClassesDescribed() throws Exception {
        ClassLoader loader = userAccount();

        assertThat(describe(call(loader, "InvalidRequestProblemV1Docs", "fields"))).containsExactly(
                "type String PLACEHOLDER", "title String PLACEHOLDER", "status Number PLACEHOLDER",
                "detail String PLACEHOLDER", "instance String optional PLACEHOLDER",
                "errors Array optional PLACEHOLDER", "errors[].message String optional PLACEHOLDER",
                "errors[].context String optional PLACEHOLDER");
        assertThat(describe(call(loader, "UserV1Docs", "fields"))).containsExactly(
                "username String PLACEHOLDER", "emailAddress String PLACEHOLDER");
        assertThat(describe(call(loader, "UserV1Docs", "fields", "user."))).containsExactly(
                "user.username String PLACEHOLDER", "user.emailAddress String PLACEHOLDER");

        assertThat(call(loader, "UserV1Docs", "responseFields")).isInstanceOf(ResponseFieldsSnippet.class);
        assertThat(relaxed(call(loader, "UserV1Docs", "responseFields"))).isFalse();
        assertThat(call(loader, "UserRegistrationRequestV1Docs", "requestFields"))
                .isInstanceOf(RequestFieldsSnippet.class);
        assertThat(relaxed(call(loader, "UserRegistrationRequestV1Docs", "requestFields"))).isFalse();
        // additionalProperties: true on ProblemDetailsV1 makes every problem open.
        assertThat(relaxed(call(loader, "UserNotFoundProblemV1Docs", "responseFields"))).isTrue();
        assertThat(relaxed(call(loader, "UserNotFoundProblemV1Docs", "requestFields"))).isTrue();

        // The inline root response's map of dependencies is a subsection.
        assertThat(describe(call(loader, "GetRootResponse200Docs", "fields")))
                .contains("dependencies Object optional subsection PLACEHOLDER");

        // No companion for what is not a public body.
        assertThat(Files.exists(directory.resolve("sources/com/example/contract/restdocs/ProblemDetailsV1Docs.java")))
                .isFalse();
        assertThat(Files.exists(directory.resolve("sources/com/example/contract/restdocs/UsernameV1Docs.java")))
                .isFalse();
        assertThat(report.degraded()).isEmpty();
    }

    @Test
    void everyJsonTypeAndADelegatingResponseAreDescribed() throws Exception {
        Path contract = directory.resolve("openapi.yaml");
        Files.writeString(contract, """
                openapi: 3.1.0
                info: {title: T, version: 2.0.0}
                paths:
                  /n:
                    get:
                      operationId: getN
                      responses:
                        '200':
                          $ref: '#/components/responses/Found'
                components:
                  schemas:
                    Node:
                      x-fragment-path: openapi/Node.yaml
                      type: object
                      required: [flag]
                      properties:
                        flag: {type: boolean}
                        nothing: {type: 'null'}
                        count: {type: integer}
                        tags: {type: array, items: {type: string}}
                        anything: {}
                        next: {$ref: '#/components/schemas/Node'}
                  responses:
                    Found:
                      x-fragment-path: openapi/responses/Found.yaml
                      description: found
                      content:
                        application/json:
                          schema: {$ref: '#/components/schemas/Node'}
                """);
        ClassLoader loader = generate(contract, "2.0.0");

        List<String> node = describe(call(loader, "NodeDocs", "fields"));
        assertThat(node).startsWith("flag Boolean PLACEHOLDER", "nothing Null optional PLACEHOLDER",
                "count Number optional PLACEHOLDER", "tags Array optional PLACEHOLDER",
                "anything Varies optional PLACEHOLDER", "next Object optional PLACEHOLDER");
        assertThat(node).contains("next.next.next Object optional subsection PLACEHOLDER");
        assertThat(describe(call(loader, "FoundDocs", "fields"))).isEqualTo(node);
        assertThat(relaxed(call(loader, "FoundDocs", "responseFields"))).isFalse();
    }

    @Test
    void aDegradedCoreMethodIsNotHiddenByTheCompanion() throws Exception {
        Path contract = directory.resolve("openapi.yaml");
        Files.writeString(contract, """
                openapi: 3.1.0
                info: {title: T, version: 1.0.0}
                paths:
                  /u:
                    get:
                      responses:
                        '200':
                          content:
                            application/json:
                              schema: {$ref: '#/components/schemas/Union'}
                components:
                  schemas:
                    Union:
                      x-fragment-path: openapi/Union.yaml
                      type: object
                      properties:
                        e: {type: [string, 'null']}
                """);
        ClassLoader loader = generate(contract, "1.0.0");

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> call(loader, "UnionDocs", "fields")))
                .hasRootCauseInstanceOf(UnsupportedOperationException.class)
                .rootCause().hasMessageContaining("MULTIPLE_TYPES");
        assertThat(JsonFieldType.VARIES).isNotNull();
    }
}
