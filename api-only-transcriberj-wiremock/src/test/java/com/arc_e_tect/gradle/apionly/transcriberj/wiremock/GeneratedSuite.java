package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import com.arc_e_tect.gradle.apionly.transcriberj.spi.Settings;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * The harness: a contract generated with the REST Docs emitter's contract tests and this
 * emitter's stubs in both formats, the generated sources compiled, concrete classes compiled for
 * every generated test interface, and the lot run in-process through the JUnit Platform Launcher
 * against a {@link DoubleServer}, collecting each test's outcome and the server's request journal.
 *
 * <p>Two variants of concrete class are compiled: {@code Preloaded}, whose hooks do nothing, for a
 * double that has loaded the mapping files; and {@code Registering}, whose hooks register the
 * case's mapping from the {@code java} format's {@code ContractStubs}.
 */
final class GeneratedSuite {

    /** The package the concrete classes are compiled into. */
    static final String HARNESS_PACKAGE = "harness";

    /** What the REST Docs emitter's test interfaces are named with, after the operation's stem. */
    static final String TESTS_SUFFIX = "ContractTests";

    /** The concrete classes. */
    enum Variant {
        /** Hooks that do nothing: the double holds every stub already. */
        PRELOADED,
        /** Hooks that register the case's mapping. */
        REGISTERING;

        String suffix() {
            return this == PRELOADED ? "Preloaded" : "Registering";
        }
    }

    /**
     * One test's outcome.
     *
     * @param testClass the concrete class's simple name
     * @param method    the test method
     * @param failure   why it failed, or null when it passed
     */
    record Outcome(String testClass, String method, Throwable failure) {

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
     * @param arranged the name of every mapping the hooks registered, in order
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

        String kind() {
            return json.get("kind").stringValue();
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

        /** The value of one of the case's fields, as text, or null. */
        String field(String name) {
            JsonNode value = json.get(name);
            return value == null || value.isNull() ? null : value.asString();
        }

        @Override
        public String toString() {
            return tests + " " + id();
        }
    }

    final String name;
    final Fixtures.Contract contract;
    final Path document;
    final Settings settings;
    final Fixtures.Generated generated;
    final JsonNode report;
    final Oracle oracle;
    final List<Case> cases = new ArrayList<>();
    final List<String> interfaces = new ArrayList<>();
    final Set<String> stateful = new HashSet<>();
    final List<Diagnostic<? extends JavaFileObject>> diagnostics = new ArrayList<>();
    private final Path directory;
    private final Path classes;
    private final URLClassLoader loader;
    private final Map<Variant, List<Class<?>>> concrete = new LinkedHashMap<>();

    private GeneratedSuite(Fixtures.Contract contract, Path document, Map<String, String> wiremock, Path directory) {
        this.name = contract.name();
        this.contract = contract;
        this.document = document;
        this.settings = contract.settings(Fixtures.TESTS_ON, wiremock);
        this.directory = directory;
        this.generated = Fixtures.generate(document, contract, Fixtures.TESTS_ON, wiremock, directory);
        this.report = Oracle.JSON.readTree(generated.report().renderValidValues(settings.contract(), "1.0.0"));
        this.oracle = new Oracle(document);
        for (JsonNode operation : report.get("contractCases")) {
            JsonNode list = operation.get("cases");
            if (list.isEmpty()) continue;
            String tests = stem(operation) + TESTS_SUFFIX;
            interfaces.add(tests);
            for (int i = 0; i < list.size(); i++) {
                cases.add(new Case(operation, list.get(i), i, tests));
                if (list.get(i).get("requiresState").booleanValue()) stateful.add(tests);
            }
        }
        try {
            classes = Files.createDirectories(directory.resolve("classes"));
            compile(generated.sources(), classes, System.getProperty("java.class.path"), diagnostics);
            Path harness = directory.resolve("harness");
            for (String tests : interfaces) {
                for (Variant variant : Variant.values()) writeConcrete(harness, tests, variant);
            }
            if (!interfaces.isEmpty()) {
                compile(List.of(harness), classes, System.getProperty("java.class.path") + File.pathSeparator + classes,
                        null);
            }
            loader = new URLClassLoader(new URL[]{classes.toUri().toURL()}, GeneratedSuite.class.getClassLoader());
            for (Variant variant : Variant.values()) {
                List<Class<?>> loaded = new ArrayList<>();
                for (String tests : interfaces) {
                    loaded.add(Class.forName(HARNESS_PACKAGE + "." + tests + variant.suffix(), true, loader));
                }
                concrete.put(variant, loaded);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A fixture contract, generated with contract tests, and stubs with the options given. */
    static GeneratedSuite of(Fixtures.Contract contract, Map<String, String> wiremock, Path directory) {
        return new GeneratedSuite(contract, contract.document(), wiremock, directory);
    }

    /** A contract document, generated as a fixture contract of that name is. */
    static GeneratedSuite of(Fixtures.Contract contract, Path document, Map<String, String> wiremock, Path directory) {
        return new GeneratedSuite(contract, document, wiremock, directory);
    }

    /** An operation's class names' stem: its case class's name without {@code ContractCases}. */
    static String stem(JsonNode operation) {
        String casesClass = operation.get("class").stringValue();
        return casesClass.substring(0, casesClass.length() - "ContractCases".length());
    }

    /** A generated class, loaded. */
    Class<?> type(String qualifiedName) {
        try {
            return Class.forName(qualifiedName, true, loader);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The class loader of the generated classes. */
    ClassLoader loader() {
        return loader;
    }

    /** Where the classes are compiled to. */
    Path classes() {
        return classes;
    }

    /** The generated stubs class, of the {@code java} format. */
    Class<?> stubs() {
        return type(settings.basePackage() + "." + WireMockEmitter.ID + "." + Sources.STUBS);
    }

    /** A case as the generated {@code CASES} holds it. */
    Object contractCase(Case c) {
        try {
            return ((List<?>) type(settings.basePackage() + "." + c.casesClass()).getField("CASES").get(null))
                    .get(c.index());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A case's request, read from the generated {@code CASES}. */
    Replay.Request request(Case c) {
        return Replay.of(contractCase(c));
    }

    /** The {@code java} format's mapping for a case. */
    MappingBuilder mapping(Case c) {
        return (MappingBuilder) invoke("mappingFor", type(settings.basePackage() + ".ContractCase"), contractCase(c));
    }

    /** The {@code java} format's fallbacks. */
    @SuppressWarnings("unchecked")
    List<MappingBuilder> fallbacks() {
        return (List<MappingBuilder>) invoke("fallbacks", null, null);
    }

    /** The {@code java} format's every mapping. */
    @SuppressWarnings("unchecked")
    List<MappingBuilder> all() {
        return (List<MappingBuilder>) invoke("all", null, null);
    }

    private Object invoke(String method, Class<?> type, Object argument) {
        try {
            return type == null ? stubs().getMethod(method).invoke(null)
                    : stubs().getMethod(method, type).invoke(null, argument);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException r) throw r;
            throw new IllegalStateException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The files directory. */
    Path files() {
        return generated.files();
    }

    /** Every mapping file, by its path under the files directory. */
    Map<String, String> mappingFiles() {
        Map<String, String> out = new TreeMap<>();
        Fixtures.Generated.files(files()).forEach((path, text) -> {
            if (path.startsWith(WireMockEmitter.MAPPINGS + "/")) out.put(path, text);
        });
        return out;
    }

    /** Every mapping file, read by WireMock, by the mapping's name. */
    Map<String, StubMapping> stubMappings() {
        Map<String, StubMapping> out = new TreeMap<>();
        mappingFiles().values().forEach(text -> {
            StubMapping mapping = StubMapping.buildFrom(text);
            out.put(mapping.getName(), mapping);
        });
        return out;
    }

    /** The mapping file of a case, read by WireMock. */
    StubMapping stubMapping(Case c) {
        return StubMapping.buildFrom(mappingFiles().get(mappingFile(c)));
    }

    /** A case's mapping file, by its path under the files directory. */
    String mappingFile(Case c) {
        return WireMockEmitter.MAPPINGS + "/" + operationName(c.operation()) + "/" + c.id() + ".json";
    }

    /** A mapping written as WireMock writes it, as a tree. */
    static JsonNode tree(StubMapping mapping) {
        return Oracle.JSON.readTree(Json.write(mapping));
    }

    /**
     * An operation's name, worked out from the contract rather than the generated code: its
     * {@code operationId}, or its class stem without one.
     */
    String operationName(JsonNode operation) {
        JsonNode id = oracle.document.at(operation.get("location").stringValue()).path("operationId");
        return id.isString() ? id.stringValue() : stem(operation);
    }

    /** The name a case's mappings must have: {@code <operation>/<caseId>}. */
    String mappingName(Case c) {
        return operationName(c.operation()) + "/" + c.id();
    }

    /** The case with an id, in the one operation that has it. */
    Case find(String id) {
        return find("", id);
    }

    /** The case with an id, in the one operation whose interface's name starts with {@code operation}. */
    Case find(String operation, String id) {
        List<Case> found = cases.stream().filter(c -> c.id().equals(id) && c.tests().startsWith(operation)).toList();
        if (found.size() != 1) throw new IllegalArgumentException(found.size() + " cases have id " + id);
        return found.get(0);
    }

    /** The cases of a kind. */
    List<Case> cases(String kind) {
        return cases.stream().filter(c -> c.kind().equals(kind)).toList();
    }

    /**
     * Runs the suite against a double.
     *
     * @param server  the double, holding whatever it needs beyond the catch-all
     * @param variant which concrete classes to run
     * @param client  how the registering hooks reach the double
     * @param headers the headers the tests' client adds to every request, as a project's defaults
     * @return the run
     */
    Run run(DoubleServer server, Variant variant, Harness.Client client, Map<String, String> headers) {
        Harness.configure(server.server, client, directory.resolve("snippets").toString(), headers);
        server.forgetRequests();
        Map<String, Outcome> outcomes = Collections.synchronizedMap(new LinkedHashMap<>());
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(concrete.get(variant).stream().map(DiscoverySelectors::selectClass).toList())
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
                            source.getMethodName(), result.getStatus() == TestExecutionResult.Status.SUCCESSFUL ? null
                            : result.getThrowable().orElse(new AssertionError(result.getStatus()))));
                }
            });
        } finally {
            thread.setContextClassLoader(previous);
        }
        return new Run(Map.copyOf(outcomes), Harness.arranged(), server.journal());
    }

    /** Runs the preloaded classes, their hooks doing nothing, against a double holding the files. */
    Run runPreloaded(DoubleServer server) {
        return run(server, Variant.PRELOADED, Harness.Client.SERVER, Map.of());
    }

    private void writeConcrete(Path harness, String tests, Variant variant) throws IOException {
        String pkg = settings.basePackage();
        String name = tests + variant.suffix();
        String hooks;
        if (variant == Variant.PRELOADED) {
            hooks = !stateful.contains(tests) ? "" : """

                        @Override
                        public void arrangeState(ContractCase contractCase) {
                            // Nothing to arrange: the published stubs already hold every case.
                        }
                    """;
        } else {
            hooks = """

                        @Override
                        public void arrangeStatelessCase(ContractCase contractCase) {
                            Harness.register(ContractStubs.mappingFor(contractCase));
                        }
                    """ + (!stateful.contains(tests) ? "" : """

                        @Override
                        public void arrangeState(ContractCase contractCase) {
                            Harness.register(ContractStubs.mappingFor(contractCase));
                        }
                    """);
        }
        String source = """
                package %1$s;

                import com.arc_e_tect.gradle.apionly.transcriberj.wiremock.Harness;
                import %2$s.ContractCase;
                import %2$s.restdocs.%3$s;
                import %2$s.wiremock.ContractStubs;
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
                        for (var header : Harness.defaultHeaders().entrySet()) {
                            builder = builder.defaultHeader(header.getKey(), header.getValue());
                        }
                        client = builder.build();
                    }

                    @Override
                    public WebTestClient client() {
                        return client;
                    }
                %5$s}
                """.formatted(HARNESS_PACKAGE, pkg, tests, name, hooks);
        Path file = harness.resolve(HARNESS_PACKAGE).resolve(name + ".java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, source);
    }

    /**
     * Compiles every source under the roots; with every lint on when diagnostics are collected,
     * as for generated sources.
     */
    static void compile(List<Path> roots, Path classes, String classpath,
                        List<Diagnostic<? extends JavaFileObject>> diagnostics) throws IOException {
        List<Path> files = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.isDirectory(root)) continue;
            try (Stream<Path> walk = Files.walk(root)) {
                files.addAll(walk.filter(p -> p.toString().endsWith(".java")).toList());
            }
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        List<String> options = new ArrayList<>(List.of("-d", classes.toString(), "-classpath", classpath,
                "--release", "21", "-proc:none"));
        if (diagnostics != null) options.addAll(List.of("-Xlint:all", "-Xdoclint:all,-missing"));
        try (StandardJavaFileManager fileManager =
                     compiler.getStandardFileManager(collector, null, StandardCharsets.UTF_8)) {
            boolean ok = compiler.getTask(null, fileManager, collector, options, null,
                    fileManager.getJavaFileObjectsFromPaths(files)).call();
            if (diagnostics != null) diagnostics.addAll(collector.getDiagnostics());
            if (!ok) {
                throw new IllegalStateException("Compiling " + roots + " failed: " + collector.getDiagnostics());
            }
        }
    }
}
