package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import com.arc_e_tect.gradle.apionly.transcriberj.spi.Settings;
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
 * The harness: a contract generated with rendering on, its generated sources compiled, a
 * concrete class compiled for every generated interface, and the lot run in-process through the
 * JUnit Platform Launcher against a {@link ContractServer}, collecting each test's outcome, its
 * failure, and the snippets written.
 */
final class GeneratedSuite {

    /** The package the concrete classes are compiled into. */
    static final String HARNESS_PACKAGE = "harness";

    /** Rendering on. */
    static final Map<String, String> ON = Map.of(InvalidRequestTests.OPTION, "true");

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
     * @param outcomes     every test's outcome, by concrete class and method: {@code Class#method}
     * @param snippets     where the snippets were written
     * @param arrangements every call of the fixture hook
     * @param received     every request the server received
     */
    record Run(Map<String, Outcome> outcomes, Path snippets, List<Harness.Arrangement> arrangements,
               List<ContractServer.Received> received) {

        List<Outcome> failed() {
            return outcomes.values().stream().filter(o -> !o.passed()).toList();
        }
    }

    /**
     * A case, as the machine-readable report records it, and where its test is.
     *
     * @param operation the operation's entry in the report
     * @param json      the case
     * @param index     its position in the operation's {@code CASES}
     * @param tests     the interface its test is in
     * @param method    its test method
     */
    record Case(JsonNode operation, JsonNode json, int index, String tests, String method) {

        String id() {
            return json.get("id").stringValue();
        }

        /** The concrete class and method that run it: {@code Class#method}. */
        String key(boolean recording) {
            return tests + (recording ? "Recording" : "Plain") + "#" + method;
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
    private final List<Class<?>> recording = new ArrayList<>();
    private final List<Class<?>> plain = new ArrayList<>();
    private int runs;

    private GeneratedSuite(String name, Path document, Settings settings, Path directory) {
        this.name = name;
        this.document = document;
        this.settings = settings;
        this.directory = directory;
        this.generated = Fixtures.generate(document, settings, directory, List.of(new RestDocsEmitter()));
        this.report = Oracle.JSON.readTree(generated.report().renderValidValues(settings.contract(), "1.0.0"));
        this.oracle = new Oracle(document);
        for (JsonNode operation : report.get("invalidRequests")) {
            String casesClass = operation.get("class").stringValue();
            String tests = casesClass.substring(0, casesClass.length() - "InvalidRequests".length())
                    + InvalidRequestTests.SUFFIX;
            JsonNode list = operation.get("cases");
            if (list.isEmpty()) continue;
            interfaces.add(tests);
            java.util.Set<String> methods = new java.util.HashSet<>();
            for (int i = 0; i < list.size(); i++) {
                JsonNode c = list.get(i);
                String method = InvalidRequestTests.variableName(c.get("id").stringValue()) + "_returns"
                        + c.get("expectedStatus").asInt();
                if (!methods.add(method)) method = method + "_" + i;
                cases.add(new Case(operation, c, i, tests, method));
            }
        }
        try {
            Path classes = Files.createDirectories(directory.resolve("classes"));
            compile(generated.sources(), classes, System.getProperty("java.class.path"), true);
            Path harness = directory.resolve("harness");
            for (String tests : interfaces) {
                writeConcrete(harness, tests, true);
                writeConcrete(harness, tests, false);
            }
            compile(harness, classes, System.getProperty("java.class.path") + File.pathSeparator + classes, false);
            loader = new URLClassLoader(new URL[]{classes.toUri().toURL()}, GeneratedSuite.class.getClassLoader());
            for (String tests : interfaces) {
                recording.add(Class.forName(HARNESS_PACKAGE + "." + tests + "Recording", true, loader));
                plain.add(Class.forName(HARNESS_PACKAGE + "." + tests + "Plain", true, loader));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A fixture contract, generated with rendering on. */
    static GeneratedSuite of(Fixtures.Contract contract, Path directory) {
        return new GeneratedSuite(contract.name(), contract.document(), contract.settings(ON), directory);
    }

    /** A contract document, generated with rendering on and the invalid-request defaults. */
    static GeneratedSuite of(String name, Path document, Path directory) {
        return new GeneratedSuite(name, document, new Fixtures.Contract(name, null, List.of()).settings(ON),
                directory);
    }

    /** A generated class, loaded. */
    Class<?> type(String qualifiedName) {
        try {
            return Class.forName(qualifiedName, true, loader);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * A case's request, as the generated {@code CASES} holds it.
     *
     * @param method         the method
     * @param pathTemplate   the path template
     * @param pathParameters the path values
     * @param query          the query parameters, each {@code name=value}
     * @param headers        the headers, each {@code name=value}
     * @param contentType    the content type, or null
     * @param body           the body's text, or null
     */
    record Request(String method, String pathTemplate, List<String> pathParameters, List<String> query,
                   List<String> headers, String contentType, String body) {
    }

    /** A case's request, read from the generated {@code CASES}: exactly what its test must send. */
    Request request(Case c) {
        try {
            String casesClass = c.operation().get("class").stringValue();
            Object invalid = ((List<?>) type(settings.basePackage() + "." + casesClass).getField("CASES").get(null))
                    .get(c.index());
            Object request = invoke(invalid, "request");
            @SuppressWarnings("unchecked")
            List<String> path = (List<String>) invoke(request, "pathParameters");
            return new Request((String) invoke(request, "method"), (String) invoke(request, "pathTemplate"), path,
                    pairs(invoke(request, "query")), pairs(invoke(request, "headers")),
                    (String) invoke(request, "contentType"), (String) invoke(request, "body"));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<String> pairs(Object pairs) throws ReflectiveOperationException {
        List<String> out = new ArrayList<>();
        for (Object pair : (List<?>) pairs) out.add(invoke(pair, "name") + "=" + invoke(pair, "value"));
        return out;
    }

    private static Object invoke(Object target, String accessor) throws ReflectiveOperationException {
        return target.getClass().getMethod(accessor).invoke(target);
    }

    /**
     * The directory a case's snippets are written in, under a run's snippets:
     * {@code <prefix>/<operationId>/<caseId>}, the operation's class's name without {@code Operation}
     * standing in for an operation without an id.
     */
    String snippetDirectory(String prefix, Case c) {
        JsonNode id = oracle.document.at(c.operation().get("location").stringValue()).path("operationId");
        String casesClass = c.operation().get("class").stringValue();
        String operationId = id.isString() ? id.stringValue()
                : casesClass.substring(0, casesClass.length() - "InvalidRequests".length());
        return prefix + "/" + operationId + "/" + c.id();
    }

    /** A generated source file, by its path under the source root. */
    Path source(String path) {
        return generated.sources().resolve(path);
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

    /** Runs the recording classes against a server. */
    Run run(ContractServer server) {
        return run(server, true, null);
    }

    /**
     * Runs the suite against a server.
     *
     * @param server    the server, answering as it has been told to
     * @param recording whether to run the classes that record the fixture hook, or those that keep
     *                  its default
     * @param prefix    the documentation prefix to override with, or null for the default
     */
    Run run(ContractServer server, boolean recording, String prefix) {
        Path snippets = directory.resolve("snippets-" + (++runs));
        Harness.configure(server.baseUrl(), snippets.toString(), prefix);
        server.clear();
        Map<String, Outcome> outcomes = Collections.synchronizedMap(new LinkedHashMap<>());
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors((recording ? this.recording : plain).stream().map(DiscoverySelectors::selectClass).toList())
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
        return new Run(Map.copyOf(outcomes), snippets, Harness.arrangements(), server.received());
    }

    private void writeConcrete(Path harness, String tests, boolean recording) throws IOException {
        String pkg = settings.basePackage();
        String name = tests + (recording ? "Recording" : "Plain");
        String arrange = recording ? """

                    @Override
                    public void arrangeInvalidRequest(InvalidRequestCase invalid) {
                        Harness.arranged(getClass().getSimpleName(), invalid.id());
                    }
                """ : "";
        String source = """
                package %1$s;

                import com.arc_e_tect.gradle.apionly.transcriberj.restdocs.Harness;
                import %2$s.InvalidRequestCase;
                import %2$s.restdocs.%3$s;
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
                        client = WebTestClient.bindToServer(Harness.connector()).baseUrl(Harness.baseUrl())
                                .filter(documentationConfiguration(provider)).build();
                    }

                    @Override
                    public WebTestClient client() {
                        return client;
                    }
                %5$s
                    @Override
                    public String generatedDocumentationPrefix() {
                        String prefix = Harness.prefix();
                        return prefix == null ? %3$s.super.generatedDocumentationPrefix() : prefix;
                    }
                }
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
