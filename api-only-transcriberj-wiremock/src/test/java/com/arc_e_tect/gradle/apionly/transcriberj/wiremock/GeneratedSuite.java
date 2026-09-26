package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import com.arc_e_tect.gradle.apionly.transcriberj.restdocs.RestDocsEmitter;
import com.arc_e_tect.gradle.apionly.transcriberj.spi.Settings;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import tools.jackson.databind.JsonNode;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The harness: a contract generated with the REST Docs emitter's rendered tests and this
 * emitter's stubs, its generated sources compiled, a concrete class compiled for every generated
 * test interface, and the lot run in-process through the JUnit Platform Launcher against a
 * {@link DoubleServer}, collecting each test's outcome and the server's request journal.
 *
 * <p>Adapted from the REST Docs emitter's own harness: the server is a real WireMock rather than
 * a JDK {@code HttpServer}, and the concrete classes' fixture hook registers the case's mapping.
 */
final class GeneratedSuite {

    /** The package the concrete classes are compiled into. */
    static final String HARNESS_PACKAGE = "harness";

    /** What the REST Docs emitter's test interfaces are named with, after the operation's stem. */
    static final String TESTS_SUFFIX = "InvalidRequestContractTests";

    /**
     * One test's outcome.
     *
     * @param testClass   the concrete class's simple name
     * @param method      the test method
     * @param displayName its display name
     * @param failure     why it failed, or null when it passed
     */
    record Outcome(String testClass, String method, String displayName, Throwable failure) {

        boolean passed() {
            return failure == null;
        }

        String message() {
            return failure == null ? null : String.valueOf(failure.getMessage());
        }
    }

    /**
     * One run of the suite.
     *
     * @param outcomes every test's outcome, by concrete class and method: {@code Class#method}
     * @param arranged the name of every mapping the fixture hook registered, in order
     * @param journal  every request the server received, in order, and the stub that answered it
     */
    record Run(Map<String, Outcome> outcomes, List<String> arranged, List<DoubleServer.Served> journal) {

        List<Outcome> failed() {
            return outcomes.values().stream().filter(o -> !o.passed()).toList();
        }
    }

    /**
     * A case, as the machine-readable report records it.
     *
     * @param operation the operation's entry in the report
     * @param json      the case
     * @param index     its position in the operation's {@code CASES}
     * @param tests     the REST Docs interface its test is in
     */
    record Case(JsonNode operation, JsonNode json, int index, String tests) {

        String id() {
            return json.get("id").stringValue();
        }

        int status() {
            return json.get("expectedStatus").asInt();
        }

        String location() {
            return operation.get("location").stringValue();
        }

        String casesClass() {
            return operation.get("class").stringValue();
        }
    }

    final String name;
    final Path document;
    final Settings settings;
    final Fixtures.Generated generated;
    final JsonNode report;
    final Oracle oracle;
    final List<Case> cases = new ArrayList<>();
    final List<String> interfaces = new ArrayList<>();
    final List<Diagnostic<? extends JavaFileObject>> diagnostics = new ArrayList<>();
    private final Path directory;
    private final URLClassLoader loader;
    private final List<Class<?>> stubbing = new ArrayList<>();
    private final List<Class<?>> plain = new ArrayList<>();

    private GeneratedSuite(Fixtures.Contract contract, Path document, Map<String, String> wiremock, Path directory) {
        this.name = contract.name();
        this.document = document;
        this.settings = contract.settings(Fixtures.TESTS_ON, wiremock);
        this.directory = directory;
        this.generated = Fixtures.generate(document, settings, directory,
                List.of(new RestDocsEmitter(), new WireMockEmitter()));
        this.report = Oracle.JSON.readTree(generated.report().renderValidValues(settings.contract(), "1.0.0"));
        this.oracle = new Oracle(document);
        for (JsonNode operation : report.get("invalidRequests")) {
            String casesClass = operation.get("class").stringValue();
            String tests = casesClass.substring(0, casesClass.length() - "InvalidRequests".length()) + TESTS_SUFFIX;
            JsonNode list = operation.get("cases");
            if (list.isEmpty()) continue;
            interfaces.add(tests);
            for (int i = 0; i < list.size(); i++) cases.add(new Case(operation, list.get(i), i, tests));
        }
        try {
            Path classes = Files.createDirectories(directory.resolve("classes"));
            compile(generated.sources(), classes, System.getProperty("java.class.path"), true);
            Path harness = directory.resolve("harness");
            for (String tests : interfaces) {
                writeConcrete(harness, tests, true);
                writeConcrete(harness, tests, false);
            }
            if (!interfaces.isEmpty()) {
                compile(harness, classes, System.getProperty("java.class.path") + File.pathSeparator + classes, false);
            }
            loader = new URLClassLoader(new URL[]{classes.toUri().toURL()}, GeneratedSuite.class.getClassLoader());
            for (String tests : interfaces) {
                stubbing.add(Class.forName(HARNESS_PACKAGE + "." + tests + "Stubbing", true, loader));
                plain.add(Class.forName(HARNESS_PACKAGE + "." + tests + "Plain", true, loader));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A fixture contract, generated with rendered tests, and stubs with the options given. */
    static GeneratedSuite of(Fixtures.Contract contract, Map<String, String> wiremock, Path directory) {
        return new GeneratedSuite(contract, contract.document(), wiremock, directory);
    }

    /** A contract document, generated as a fixture contract of that name is. */
    static GeneratedSuite of(Fixtures.Contract contract, Path document, Map<String, String> wiremock, Path directory) {
        return new GeneratedSuite(contract, document, wiremock, directory);
    }

    /** A generated class, loaded. */
    Class<?> type(String qualifiedName) {
        try {
            return Class.forName(qualifiedName, true, loader);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The generated stubs class. */
    Class<?> stubs() {
        return type(settings.basePackage() + "." + WireMockEmitter.ID + "." + Sources.STUBS);
    }

    /** A case as the generated {@code CASES} holds it. */
    Object invalid(Case c) {
        try {
            return ((List<?>) type(settings.basePackage() + "." + c.casesClass()).getField("CASES").get(null))
                    .get(c.index());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The generated mapping for a case. */
    MappingBuilder mapping(Case c) {
        return mapping(invalid(c));
    }

    /** The generated mapping for a case as the generated {@code InvalidRequestCase} holds it. */
    MappingBuilder mapping(Object invalid) {
        try {
            return (MappingBuilder) stubs().getMethod("mappingFor", type(settings.basePackage() + ".InvalidRequestCase"))
                    .invoke(null, invalid);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException r) throw r;
            throw new IllegalStateException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The name a case's mapping must have, worked out from the contract rather than the case:
     * {@code <operationId>/<caseId>}, or {@code <METHOD> <path>/<caseId>} without an id.
     */
    String mappingName(Case c) {
        JsonNode id = oracle.document.at(c.location()).path("operationId");
        String operation = id.isString() ? id.stringValue()
                : c.operation().get("method").stringValue() + " " + c.operation().get("pathTemplate").stringValue();
        return operation + "/" + c.id();
    }

    /** A generated source file, by its path under the source root. */
    Path source(String path) {
        return generated.sources().resolve(path);
    }

    /** The cases of one operation, by its entry in the report. */
    List<Case> casesOf(JsonNode operation) {
        return cases.stream().filter(c -> c.operation() == operation).toList();
    }

    /** The operations with cases, as the report records them. */
    List<JsonNode> operations() {
        List<JsonNode> out = new ArrayList<>();
        for (JsonNode operation : report.get("invalidRequests")) {
            if (!operation.get("cases").isEmpty()) out.add(operation);
        }
        return out;
    }

    /** The case with an id, in the one operation that has it. */
    Case find(String id) {
        List<Case> found = cases.stream().filter(c -> c.id().equals(id)).toList();
        if (found.size() != 1) throw new IllegalArgumentException(found.size() + " cases have id " + id);
        return found.get(0);
    }

    /**
     * Runs the suite against a double.
     *
     * @param server   the double; the caller has registered whatever it needs beyond the catch-all
     * @param stubbing whether to run the classes whose fixture hook registers the case's mapping, or
     *                 those that keep the default, which does nothing
     * @param client   how the hook registers the mapping
     * @param defaults whether the tests' client adds the reference implementation's default headers
     */
    Run run(DoubleServer server, boolean stubbing, Harness.Client client, boolean defaults) {
        Harness.configure(server.server, client, directory.resolve("snippets").toString(), defaults);
        server.forgetRequests();
        Map<String, Outcome> outcomes = Collections.synchronizedMap(new LinkedHashMap<>());
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors((stubbing ? this.stubbing : plain).stream().map(DiscoverySelectors::selectClass).toList())
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false")
                .build();
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        thread.setContextClassLoader(loader);
        try {
            Launcher launcher = LauncherFactory.create();
            launcher.execute(request, new TestExecutionListener() {
                @Override
                public void executionFinished(TestIdentifier id, TestExecutionResult result) {
                    if (!id.isTest()) {
                        if (result.getStatus() != TestExecutionResult.Status.SUCCESSFUL) {
                            throw new IllegalStateException("A container failed: " + id.getDisplayName(),
                                    result.getThrowable().orElse(null));
                        }
                        return;
                    }
                    MethodSource source = (MethodSource) id.getSource().orElseThrow();
                    String testClass = source.getClassName().substring(HARNESS_PACKAGE.length() + 1);
                    outcomes.put(testClass + "#" + source.getMethodName(), new Outcome(testClass,
                            source.getMethodName(), id.getDisplayName(),
                            result.getStatus() == TestExecutionResult.Status.SUCCESSFUL ? null
                                    : result.getThrowable().orElse(new AssertionError(result.getStatus()))));
                }
            });
        } finally {
            thread.setContextClassLoader(previous);
        }
        return new Run(Map.copyOf(outcomes), Harness.arranged(), server.journal());
    }

    /** Runs the classes whose hook registers the case's mapping on the in-process server. */
    Run run(DoubleServer server) {
        return run(server, true, Harness.Client.SERVER, false);
    }

    private void writeConcrete(Path harness, String tests, boolean stubbing) throws IOException {
        String pkg = settings.basePackage();
        String name = tests + (stubbing ? "Stubbing" : "Plain");
        String arrange = stubbing ? """

                    @Override
                    public void arrangeInvalidRequest(InvalidRequestCase invalid) {
                        MappingBuilder mapping = InvalidRequestStubs.mappingFor(invalid);
                        switch (Harness.client()) {
                            case SERVER -> Harness.server().stubFor(mapping);
                            case STATIC -> WireMock.stubFor(mapping);
                            case INSTANCE -> Harness.instance().register(mapping);
                        }
                        Harness.arranged(mapping.build().getName());
                    }
                """ : "";
        String source = """
                package %1$s;

                import com.arc_e_tect.gradle.apionly.transcriberj.wiremock.Harness;
                import %2$s.InvalidRequestCase;
                import %2$s.restdocs.%3$s;
                import %2$s.wiremock.InvalidRequestStubs;
                import com.github.tomakehurst.wiremock.client.MappingBuilder;
                import com.github.tomakehurst.wiremock.client.WireMock;
                import org.junit.jupiter.api.BeforeEach;
                import org.junit.jupiter.api.extension.RegisterExtension;
                import org.springframework.restdocs.RestDocumentationContextProvider;
                import org.springframework.restdocs.RestDocumentationExtension;
                import org.springframework.test.web.reactive.server.WebTestClient;

                import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.documentationConfiguration;

                public class %4$s implements %3$s {

                    @RegisterExtension
                    final RestDocumentationExtension restDocumentation = new RestDocumentationExtension(Harness.snippets());

                    private WebTestClient client;

                    @BeforeEach
                    void bind(RestDocumentationContextProvider provider) {
                        WebTestClient.Builder builder = WebTestClient.bindToServer(Harness.connector())
                                .baseUrl(Harness.baseUrl()).filter(documentationConfiguration(provider));
                        if (Harness.defaultHeaders()) {
                            // As the reference implementation's WireMockContractValidatorTest sets them.
                            builder = builder.defaultHeader("Content-Type", "application/json")
                                    .defaultHeader("Accept", "application/json")
                                    .defaultHeader("Person-Agent", "API-Contract-Validator")
                                    .defaultHeader("X-Forwarded-For", "203.0.113.7");
                        }
                        client = builder.build();
                    }

                    @Override
                    public WebTestClient client() {
                        return client;
                    }
                %5$s}
                """.formatted(HARNESS_PACKAGE, pkg, tests, name, arrange);
        Path file = harness.resolve(HARNESS_PACKAGE).resolve(name + ".java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, source);
    }

    /** Compiles every source under a root; the generated ones with every lint, recording what it says. */
    private void compile(Path root, Path classes, String classpath, boolean generatedSources) throws IOException {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(root)) {
            files = walk.filter(p -> p.toString().endsWith(".java")).toList();
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        List<String> options = new ArrayList<>(List.of("-d", classes.toString(), "-classpath", classpath,
                "--release", "21", "-proc:none"));
        if (generatedSources) options.addAll(List.of("-Xlint:all", "-Xdoclint:all,-missing"));
        try (StandardJavaFileManager fileManager =
                     compiler.getStandardFileManager(collector, null, StandardCharsets.UTF_8)) {
            boolean ok = compiler.getTask(null, fileManager, collector, options, null,
                    fileManager.getJavaFileObjectsFromPaths(files)).call();
            if (generatedSources) diagnostics.addAll(collector.getDiagnostics());
            if (!ok) {
                throw new IllegalStateException("Compiling " + root + " failed: " + collector.getDiagnostics());
            }
        }
    }
}
