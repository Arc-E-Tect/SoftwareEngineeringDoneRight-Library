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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * The harness: a contract generated with rendering on, its generated sources compiled, concrete
 * classes compiled for every generated interface, and the lot run in-process through the JUnit
 * Platform Launcher against a {@link ContractServer}, collecting each test's outcome, its
 * failure, and the snippets written.
 *
 * <p>Two variants of concrete class are compiled with the suite: {@code Recording}, which records
 * every hook call and has the harness's fixture arrange state; and {@code Plain}, which overrides
 * no hook. Others, such as one whose {@code arrangeState} is the suggested implementation, are
 * compiled on demand by {@link #variant}.
 */
final class GeneratedSuite {

    /** The package the concrete classes are compiled into. */
    static final String HARNESS_PACKAGE = "harness";

    /** The concrete classes that record the hooks and arrange state. */
    static final String RECORDING = "Recording";

    /** The concrete classes that override no hook. */
    static final String PLAIN = "Plain";

    /** Rendering on. */
    static final Map<String, String> ON = Map.of(ContractTests.OPTION, "true");

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
     * @param order        the tests' keys, in the order they finished
     * @param snippets     where the snippets were written
     * @param arrangements every call of a fixture hook
     * @param received     every request the server received
     */
    record Run(Map<String, Outcome> outcomes, List<String> order, Path snippets,
               List<Harness.Arrangement> arrangements, List<ContractServer.Received> received) {

        List<Outcome> failed() {
            return outcomes.values().stream().filter(o -> !o.passed()).toList();
        }
    }

    /**
     * How to run the suite.
     *
     * @param classes the concrete classes to run
     * @param prefix  the documentation prefix to override with, or null for the default
     * @param fixture the fixture that arranges state, or null for the server's correct one
     * @param config  JUnit configuration parameters, such as a random order and its seed
     */
    record Options(List<Class<?>> classes, String prefix, Harness.Fixture fixture, Map<String, String> config) {
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

        String kind() {
            return json.get("kind").stringValue();
        }

        boolean requiresState() {
            return json.get("requiresState").booleanValue();
        }

        /** The {@code Recording} or {@code Plain} class and method that run it: {@code Class#method}. */
        String key(boolean recording) {
            return key(recording ? RECORDING : PLAIN);
        }

        /** A variant's class and method that run it: {@code Class#method}. */
        String key(String variant) {
            return tests + variant + "#" + method;
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
    final Set<String> stateful = new HashSet<>();
    final List<Diagnostic<? extends JavaFileObject>> diagnostics = new ArrayList<>();
    private final Path directory;
    private final Path classes;
    private final URLClassLoader loader;
    private final List<Class<?>> recording = new ArrayList<>();
    private final List<Class<?>> plain = new ArrayList<>();
    private int runs;
    private int variants;

    private GeneratedSuite(String name, Path document, Settings settings, Path directory) {
        this.name = name;
        this.document = document;
        this.settings = settings;
        this.directory = directory;
        this.generated = Fixtures.generate(document, settings, directory, List.of(new RestDocsEmitter()));
        this.report = Oracle.JSON.readTree(generated.report().renderValidValues(settings.contract(), "1.0.0"));
        this.oracle = new Oracle(document);
        for (JsonNode operation : report.get("contractCases")) {
            JsonNode list = operation.get("cases");
            if (list.isEmpty()) continue;
            String tests = stem(operation) + ContractTests.SUFFIX;
            interfaces.add(tests);
            Set<String> methods = new HashSet<>();
            for (int i = 0; i < list.size(); i++) {
                JsonNode c = list.get(i);
                String method = ContractTests.variableName(c.get("id").stringValue()) + "_returns"
                        + c.get("expectedStatus").asInt();
                if (!methods.add(method)) method = method + "_" + i;
                cases.add(new Case(operation, c, i, tests, method));
                if (c.get("requiresState").booleanValue()) stateful.add(tests);
            }
        }
        try {
            classes = Files.createDirectories(directory.resolve("classes"));
            compile(generated.sources(), classes, System.getProperty("java.class.path"), diagnostics);
            Path harness = directory.resolve("harness");
            for (String tests : interfaces) {
                writeConcrete(harness, tests, RECORDING, recordingMembers(tests));
                writeConcrete(harness, tests, PLAIN, "");
            }
            compile(harness, classes, System.getProperty("java.class.path") + File.pathSeparator + classes, null);
            loader = new URLClassLoader(new URL[]{classes.toUri().toURL()}, GeneratedSuite.class.getClassLoader());
            for (String tests : interfaces) {
                recording.add(Class.forName(HARNESS_PACKAGE + "." + tests + RECORDING, true, loader));
                plain.add(Class.forName(HARNESS_PACKAGE + "." + tests + PLAIN, true, loader));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    /** An operation's class names' stem: its case class's name without {@code ContractCases}. */
    static String stem(JsonNode operation) {
        String casesClass = operation.get("class").stringValue();
        return casesClass.substring(0, casesClass.length() - "ContractCases".length());
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

    /** The members of a {@code Recording} class: both hooks record, and the state hook has the fixture arrange. */
    private String recordingMembers(String tests) {
        String out = """

                    @Override
                    public void arrangeStatelessCase(ContractCase contractCase) {
                        Harness.arranged(getClass().getSimpleName(), contractCase.id());
                    }
                """;
        if (stateful.contains(tests)) {
            out += """

                        @Override
                        public void arrangeState(ContractCase contractCase) {
                            Harness.arrangeState(getClass().getSimpleName(), contractCase.id(), contractCase.kind().name(),
                                    contractCase.expectedStatus(), contractCase.request().pathTemplate(),
                                    contractCase.request().pathParameters());
                        }
                    """;
        }
        return out;
    }

    /**
     * Compiles a variant of concrete class for every interface, with the members given -- or none,
     * where the function gives null -- and every lint on, and loads them.
     *
     * @param variant     the variant's name, which the classes' names end with
     * @param members     each interface's members, by the interface's simple name
     * @param diagnostics what the compiler says
     * @return the classes
     */
    List<Class<?>> variant(String variant, Function<String, String> members,
                           List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        try {
            Path sources = directory.resolve("variant-" + (++variants));
            Path out = Files.createDirectories(directory.resolve("variant-classes-" + variants));
            for (String tests : interfaces) {
                String body = members.apply(tests);
                writeConcrete(sources, tests, variant, body == null ? "" : body);
            }
            compile(sources, out, System.getProperty("java.class.path") + File.pathSeparator + classes, diagnostics);
            URLClassLoader variantLoader = new URLClassLoader(new URL[]{out.toUri().toURL()}, loader);
            List<Class<?>> loaded = new ArrayList<>();
            for (String tests : interfaces) {
                loaded.add(Class.forName(HARNESS_PACKAGE + "." + tests + variant, true, variantLoader));
            }
            return loaded;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
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
            Object contractCase = ((List<?>) type(settings.basePackage() + "." + casesClass).getField("CASES")
                    .get(null)).get(c.index());
            Object request = invoke(contractCase, "request");
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
        String operationId = id.isString() ? id.stringValue() : stem(c.operation());
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

    /** The cases of a kind. */
    List<Case> cases(String kind) {
        return cases.stream().filter(c -> c.kind().equals(kind)).toList();
    }

    /** Runs the recording classes against a server, with the server's correct fixture. */
    Run run(ContractServer server) {
        return run(server, true, null);
    }

    /**
     * Runs the suite against a server, with the server's correct fixture.
     *
     * @param server    the server, answering as it has been told to
     * @param recording whether to run the classes that record the hooks and arrange state, or those
     *                  that keep every default
     * @param prefix    the documentation prefix to override with, or null for the default
     */
    Run run(ContractServer server, boolean recording, String prefix) {
        return run(server, new Options(recording ? this.recording : plain, prefix, null, Map.of()));
    }

    /** Runs the recording classes against a server, with a fixture and JUnit configuration. */
    Run run(ContractServer server, Harness.Fixture fixture, Map<String, String> config) {
        return run(server, new Options(recording, null, fixture, config));
    }

    /** Runs the suite against a server, as the options say. */
    Run run(ContractServer server, Options options) {
        Path snippets = directory.resolve("snippets-" + (++runs));
        Harness.Fixture fixture = options.fixture();
        if (fixture == null) {
            fixture = server.behaviour() instanceof ValidatingServer validating ? validating.store::arrange : c -> {
            };
        }
        Harness.configure(server.baseUrl(), snippets.toString(), options.prefix(), fixture);
        server.clear();
        Map<String, Outcome> outcomes = Collections.synchronizedMap(new LinkedHashMap<>());
        List<String> order = Collections.synchronizedList(new ArrayList<>());
        LauncherDiscoveryRequestBuilder builder = LauncherDiscoveryRequestBuilder.request()
                .selectors(options.classes().stream().map(DiscoverySelectors::selectClass).toList())
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false");
        options.config().forEach(builder::configurationParameter);
        LauncherDiscoveryRequest request = builder.build();
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        thread.setContextClassLoader(options.classes().isEmpty() ? loader
                : options.classes().get(0).getClassLoader());
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
                    String key = testClass + "#" + source.getMethodName();
                    order.add(key);
                    outcomes.put(key, new Outcome(testClass, source.getMethodName(), id.getDisplayName(),
                            result.getStatus() == TestExecutionResult.Status.SUCCESSFUL ? null
                                    : result.getThrowable().orElse(new AssertionError(result.getStatus()))));
                }
            });
        } finally {
            thread.setContextClassLoader(previous);
        }
        return new Run(Map.copyOf(outcomes), List.copyOf(order), snippets, Harness.arrangements(), server.received());
    }

    private void writeConcrete(Path root, String tests, String variant, String members) throws IOException {
        String pkg = settings.basePackage();
        String name = tests + variant;
        String source = """
                package %1$s;

                import com.arc_e_tect.gradle.apionly.transcriberj.restdocs.Harness;
                import %2$s.ContractCase;
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
                """.formatted(HARNESS_PACKAGE, pkg, tests, name, members);
        Path file = root.resolve(HARNESS_PACKAGE).resolve(name + ".java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, source);
    }

    /**
     * Compiles every source under a root. With a list to collect them in, with every lint on, and
     * what the compiler says collected.
     */
    private static void compile(Path root, Path classes, String classpath,
                                List<Diagnostic<? extends JavaFileObject>> diagnostics) throws IOException {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(root)) {
            files = walk.filter(p -> p.toString().endsWith(".java")).toList();
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
                throw new IllegalStateException("Compiling " + root + " failed: " + collector.getDiagnostics());
            }
        }
    }
}
