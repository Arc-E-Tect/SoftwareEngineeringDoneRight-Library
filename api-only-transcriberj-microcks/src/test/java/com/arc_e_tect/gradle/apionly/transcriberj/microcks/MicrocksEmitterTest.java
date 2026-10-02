package com.arc_e_tect.gradle.apionly.transcriberj.microcks;

import com.arc_e_tect.gradle.apionly.transcriberj.core.Generation;
import com.arc_e_tect.gradle.apionly.transcriberj.core.GenerationException;
import com.arc_e_tect.gradle.apionly.transcriberj.spi.Emitter;
import com.arc_e_tect.gradle.apionly.transcriberj.spi.ManagedDependency;
import com.arc_e_tect.gradle.apionly.transcriberj.spi.Settings;
import com.sun.net.httpserver.HttpServer;
import io.github.microcks.testcontainers.model.TestResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The emitter, run over real contracts by the TranscriberJ, and its output compiled. */
class MicrocksEmitterTest {

    private static final String PACKAGE = "com.example.contract";

    @TempDir
    Path directory;

    private Path sources;
    private Path resources;

    private void generate(Path contract, Path asyncContract, String version) {
        sources = directory.resolve("sources");
        resources = directory.resolve("resources");
        Generation.run(contract, asyncContract, version, "x",
                new Settings("test", PACKAGE, false, "PLACEHOLDER", 2), sources, resources,
                List.of(new MicrocksEmitter()), null);
    }

    private ClassLoader compile() throws Exception {
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
        return new URLClassLoader(
                new URL[]{classes.toUri().toURL(), resources.toUri().toURL()}, getClass().getClassLoader());
    }

    private ClassLoader userAccount() throws Exception {
        generate(Path.of(getClass().getResource("/contracts/user-account/openapi.yaml").toURI()),
                Path.of(getClass().getResource("/contracts/user-account/asyncapi.yaml").toURI()), "1.0.0");
        return compile();
    }

    @Test
    void theEmitterDescribesItselfAndIsAService() {
        MicrocksEmitter emitter = new MicrocksEmitter();
        assertThat(emitter.id()).isEqualTo("microcks");
        assertThat(emitter.dependencies()).containsExactly(
                new ManagedDependency("io.github.microcks", "microcks-testcontainers", "0.5.0", "0.6.0"),
                new ManagedDependency("org.testcontainers", "testcontainers", "2.0.5", "2.1.0"));
        assertThat(emitter.represents()).isEmpty();
        assertThat(ServiceLoader.load(Emitter.class)).extracting(Emitter::id).contains("microcks");
    }

    @Test
    void theHarnessCarriesOneHookAndOneTestPerSendOperation() throws Exception {
        ClassLoader loader = userAccount();
        Class<?> harness = Class.forName(PACKAGE + ".microcks.AsyncConformanceHarness", true, loader);
        assertThat(harness.isInterface()).isTrue();

        assertThat(harness.getMethod("publishRegistrationInitiated", String.class).getModifiers())
                .matches(m -> Modifier.isAbstract(m) && Modifier.isPublic(m));
        Method test = harness.getMethod("publishRegistrationInitiated_conformsToContract");
        assertThat(Modifier.isAbstract(test.getModifiers())).isFalse();
        assertThat(test.isAnnotationPresent(org.junit.jupiter.api.Test.class)).isTrue();

        assertThat(harness.getMethod("wireBroker", Class.forName(
                "io.github.microcks.testcontainers.MicrocksContainersEnsemble", true, loader))).isNotNull();
        assertThat(harness.getMethod("contractFile").getReturnType()).isEqualTo(java.nio.file.Path.class);
        assertThat(harness.getMethod("endpoint", String.class)).isNotNull();

        assertThat(harness.getAnnotation(org.junit.jupiter.api.TestInstance.class).value())
                .isEqualTo(org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS);

        List<String> operationIds = List.of("publishRegistrationInitiated", "publishVerificationEmailSent",
                "publishVerificationEmailResendRequested", "publishNewVerificationEmailSent",
                "publishUserAccountCreated", "publishConfirmationEmailSent", "publishPersonDeleted",
                "publishRegistrationAttemptedWithExpiredLink",
                "publishRegistrationAttemptedWithExpiredRegistrationProcess",
                "publishRegistrationAttemptedWithReservedUsername", "publishRegistrationAttemptedWithTakenUsername",
                "publishRegistrationAttemptedWithInvalidLink", "publishRegistrationAttemptedWithInitiatedEmailAddress",
                "publishRegistrationAttemptedWithTakenEmailAddress");
        for (String operationId : operationIds) {
            assertThat(Arrays.stream(harness.getMethods()).map(Executable::getName))
                    .as(operationId).contains(operationId, operationId + "_conformsToContract");
        }
    }

    @Test
    void aTestAsksMicrocksToListenAndWaitsBeforeItPublishes() throws Exception {
        ClassLoader loader = userAccount();
        String source = Files.readString(sources.resolve(PACKAGE.replace('.', '/') + "/microcks/AsyncConformanceHarness.java"));
        String test = source.substring(source.indexOf("default void publishRegistrationInitiated_conformsToContract"));
        test = test.substring(0, test.indexOf("\n    }\n"));

        assertThat(test.indexOf("testEndpointAsync"))
                .isPositive().isLessThan(test.indexOf("subscriptionDelay()"));
        assertThat(test.indexOf("subscriptionDelay()"))
                .isLessThan(test.indexOf("publishRegistrationInitiated(suffixedChannel)"));

        Class<?> harness = Class.forName(PACKAGE + ".microcks.AsyncConformanceHarness", true, loader);
        assertThat(harness.getMethod("subscriptionDelay").getReturnType()).isEqualTo(java.time.Duration.class);
        assertThat(java.lang.reflect.Modifier.isAbstract(harness.getMethod("subscriptionDelay").getModifiers())).isFalse();
    }

    @Test
    void howLongMicrocksListensAndHowLongATestWaitsForItAreHooks() throws Exception {
        ClassLoader loader = userAccount();
        String source = Files.readString(sources.resolve(PACKAGE.replace('.', '/') + "/microcks/AsyncConformanceHarness.java"));
        String test = source.substring(source.indexOf("default void publishRegistrationInitiated_conformsToContract"));
        test = test.substring(0, test.indexOf("\n    }\n"));
        assertThat(test).contains(".timeout(testTimeout())")
                .contains("verdict(future, resultTimeout(),")
                .doesNotContain("ofSeconds(10)").doesNotContain("15, TimeUnit");

        Class<?> harness = Class.forName(PACKAGE + ".microcks.AsyncConformanceHarness", true, loader);
        Object defaults = java.lang.reflect.Proxy.newProxyInstance(loader, new Class<?>[]{harness},
                (proxy, method, args) -> java.lang.reflect.InvocationHandler.invokeDefault(proxy, method, args));
        assertThat(harness.getMethod("testTimeout").invoke(defaults)).isEqualTo(java.time.Duration.ofSeconds(10));
        assertThat(harness.getMethod("resultTimeout").invoke(defaults)).isEqualTo(java.time.Duration.ofSeconds(15));

        // A project that only lets Microcks listen longer still waits long enough for the result.
        Object slower = java.lang.reflect.Proxy.newProxyInstance(loader, new Class<?>[]{harness},
                (proxy, method, args) -> method.getName().equals("testTimeout")
                        ? java.time.Duration.ofSeconds(60)
                        : java.lang.reflect.InvocationHandler.invokeDefault(proxy, method, args));
        assertThat(harness.getMethod("resultTimeout").invoke(slower)).isEqualTo(java.time.Duration.ofSeconds(65));
    }

    @Test
    void aTestPublishesOnceAndAssertsOnMicrocksVerdict() throws Exception {
        userAccount();
        String source = Files.readString(sources.resolve(PACKAGE.replace('.', '/') + "/microcks/AsyncConformanceHarness.java"));
        String test = source.substring(source.indexOf("default void publishRegistrationInitiated_conformsToContract"));
        test = test.substring(0, test.indexOf("\n    }\n"));

        assertThat(test.split("publishRegistrationInitiated\\(suffixedChannel\\)", -1)).hasSize(2);
        assertThat(test.indexOf("publishRegistrationInitiated(suffixedChannel)"))
                .isLessThan(test.indexOf("verdict(future"));
        assertThat(test.indexOf("verdict(future")).isLessThan(test.indexOf("Assertions.assertTrue(result.isSuccess()"));
    }

    @Test
    void aResultMicrocksHadNotFinishedIsAskedForAgainUntilItIsFinal() throws Exception {
        Class<?> harness = harness();
        try (FakeMicrocks microcks = FakeMicrocks.answering(200, IN_PROGRESS, IN_PROGRESS, CONFORMS)) {
            TestResult result = verdict(harness, inProgress(), Duration.ofSeconds(10), microcks.endpoint());

            assertThat(result.isInProgress()).isFalse();
            assertThat(result.isSuccess()).isTrue();
            assertThat(microcks.asked()).hasValue(3);
        }
    }

    @Test
    void aMessageThatDoesNotConformStillFailsWithWhatMicrocksFound() throws Exception {
        Class<?> harness = harness();
        try (FakeMicrocks microcks = FakeMicrocks.answering(200, IN_PROGRESS, DOES_NOT_CONFORM)) {
            TestResult result = verdict(harness, inProgress(), Duration.ofSeconds(10), microcks.endpoint());

            assertThat(result.isSuccess()).isFalse();
            assertThat(microcks.asked()).hasValue(2);
            assertThat(diagnostics(harness, result))
                    .isEqualTo("SEND publishRegistrationInitiated: $.username: is missing but it is required\n");
        }
    }

    @Test
    void aResultThatIsAlreadyFinalIsNotAskedForAgain() throws Exception {
        Class<?> harness = harness();
        TestResult returned = new TestResult();
        returned.setId("t1");
        returned.setInProgress(false);
        returned.setSuccess(false);
        try (FakeMicrocks microcks = FakeMicrocks.answering(200, CONFORMS)) {
            TestResult result = verdict(harness, returned, Duration.ofSeconds(10), microcks.endpoint());

            assertThat(result).isSameAs(returned);
            assertThat(microcks.asked()).hasValue(0);
        }
    }

    @Test
    void noVerdictWithinTheResultTimeoutFailsSayingMicrocksHadNotFinished() throws Exception {
        Class<?> harness = harness();
        try (FakeMicrocks microcks = FakeMicrocks.answering(200, IN_PROGRESS)) {
            long started = System.nanoTime();
            TestResult result = verdict(harness, inProgress(), Duration.ofMillis(700), microcks.endpoint());

            assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(5));
            assertThat(result.isInProgress()).isTrue();
            assertThat(result.isSuccess()).isFalse();
            assertThat(diagnostics(harness, result))
                    .contains("Microcks had not finished testing kafka://kafka:9092/audit.v1-x",
                            "no verdict", "Override resultTimeout()");
        }
    }

    @Test
    void anErrorFromMicrocksIsReportedNotTakenForAVerdict() throws Exception {
        Class<?> harness = harness();
        try (FakeMicrocks microcks = FakeMicrocks.answering(404, "{}")) {
            assertThatThrownBy(() -> verdict(harness, inProgress(), Duration.ofSeconds(10), microcks.endpoint()))
                    .isInstanceOf(java.io.IOException.class)
                    .hasMessage("Microcks answered 404 when asked for the result of test t1.");
        }
    }

    /** Microcks' answers for test {@code t1}: still running, and final both ways. */
    private static final String IN_PROGRESS = """
            {"id": "t1", "inProgress": true, "success": false, "testedEndpoint": "kafka://kafka:9092/audit.v1-x"}""";
    private static final String CONFORMS = """
            {"id": "t1", "inProgress": false, "success": true, "testedEndpoint": "kafka://kafka:9092/audit.v1-x",
             "testCaseResults": [{"operationName": "SEND publishRegistrationInitiated", "success": true,
                                  "testStepResults": [{"success": true}]}],
             "aFieldThisClientDoesNotKnow": 1}""";
    private static final String DOES_NOT_CONFORM = """
            {"id": "t1", "inProgress": false, "success": false, "testedEndpoint": "kafka://kafka:9092/audit.v1-x",
             "testCaseResults": [{"operationName": "SEND publishRegistrationInitiated", "success": false,
                                  "testStepResults": [{"success": false,
                                                       "message": "$.username: is missing but it is required"}]}]}""";

    /** What microcks-testcontainers returns when it stops waiting before Microcks has finished. */
    private static TestResult inProgress() {
        TestResult result = new TestResult();
        result.setId("t1");
        result.setInProgress(true);
        result.setTestedEndpoint("kafka://kafka:9092/audit.v1-x");
        return result;
    }

    private Class<?> harness() throws Exception {
        return Class.forName(PACKAGE + ".microcks.AsyncConformanceHarness", true, userAccount());
    }

    /** The generated harness's own {@code verdict}, on a future that has already returned {@code returned}. */
    private static TestResult verdict(Class<?> harness, TestResult returned, Duration within, String microcksEndpoint)
            throws Exception {
        Method verdict = harness.getDeclaredMethod("verdict", CompletableFuture.class, Duration.class, String.class);
        verdict.setAccessible(true);
        try {
            return (TestResult) verdict.invoke(null, CompletableFuture.completedFuture(returned), within, microcksEndpoint);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    /** The generated harness's own {@code diagnostics}. */
    private static String diagnostics(Class<?> harness, TestResult result) throws Exception {
        Method diagnostics = harness.getDeclaredMethod("diagnostics", TestResult.class);
        diagnostics.setAccessible(true);
        return (String) diagnostics.invoke(null, result);
    }

    /**
     * A stand-in for Microcks' REST API that answers {@code GET /api/tests/<id>} with each
     * of {@code answers} in turn, and with the last from then on, counting how often it was asked.
     */
    private record FakeMicrocks(HttpServer server, AtomicInteger asked) implements AutoCloseable {

        static FakeMicrocks answering(int status, String... answers) throws java.io.IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            AtomicInteger asked = new AtomicInteger();
            server.createContext("/api/tests/t1", exchange -> {
                byte[] body = answers[Math.min(asked.getAndIncrement(), answers.length - 1)]
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, body.length);
                try (var out = exchange.getResponseBody()) {
                    out.write(body);
                }
            });
            server.start();
            return new FakeMicrocks(server, asked);
        }

        String endpoint() {
            return "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    @Test
    void theMicrocksImageIsTheTestedOneUnlessAProjectOverridesIt() throws Exception {
        ClassLoader loader = userAccount();
        Class<?> harness = Class.forName(PACKAGE + ".microcks.AsyncConformanceHarness", true, loader);
        Object defaults = java.lang.reflect.Proxy.newProxyInstance(loader, new Class<?>[]{harness},
                (proxy, method, args) -> java.lang.reflect.InvocationHandler.invokeDefault(proxy, method, args));

        assertThat(harness.getMethod("microcksImage").invoke(defaults))
                .isEqualTo("quay.io/microcks/microcks-uber:1.15.0")
                .isEqualTo(harness.getField("MICROCKS_IMAGE").get(null));
    }

    @Test
    void anOverriddenImageIsWarnedAboutAndTheTestedOneIsNot() throws Exception {
        ClassLoader loader = userAccount();
        Class<?> harness = Class.forName(PACKAGE + ".microcks.AsyncConformanceHarness", true, loader);
        Method warn = harness.getDeclaredMethod("warnIfNotTheTestedImage", String.class);
        warn.setAccessible(true);
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(harness.getName());
        List<String> messages = new java.util.ArrayList<>();
        java.util.logging.Handler handler = new java.util.logging.Handler() {
            @Override public void publish(java.util.logging.LogRecord r) {
                messages.add(r.getLevel() + ": " + r.getMessage());
            }
            @Override public void flush() { }
            @Override public void close() { }
        };
        logger.addHandler(handler);
        try {
            warn.invoke(null, "quay.io/microcks/microcks-uber:1.15.0");
            assertThat(messages).isEmpty();

            warn.invoke(null, "registry.example.com/microcks:9");
            assertThat(messages).singleElement().satisfies(message -> assertThat(message)
                    .startsWith("WARNING:")
                    .contains("registry.example.com/microcks:9", "quay.io/microcks/microcks-uber:1.15.0",
                            "may not work", "run it with the default image first"));
        } finally {
            logger.removeHandler(handler);
        }
    }

    @Test
    void theContractPropertiesResourceCarriesTheServiceNameAndAPinnedMicrocksImage() throws Exception {
        userAccount();

        Path resource = resources.resolve("META-INF/apionly/microcks/test.properties");
        assertThat(resource).exists();
        Properties properties = new Properties();
        try (var in = Files.newInputStream(resource)) {
            properties.load(in);
        }
        assertThat(properties.getProperty("asyncapi.service.name")).isEqualTo("IFF Async API");
        assertThat(properties.getProperty("microcks.image")).isEqualTo("quay.io/microcks/microcks-uber:1.15.0");
    }

    @Test
    void receiveOperationsAndAContractWithoutEventsAreNotRepresented() throws Exception {
        Path openapi = directory.resolve("openapi.yaml");
        Files.writeString(openapi, "openapi: 3.1.0\ninfo: {title: T, version: 1.0.0}\n");
        generate(openapi, null, "1.0.0");
        assertThat(Files.exists(sources.resolve(PACKAGE.replace('.', '/') + "/microcks"))).isFalse();
        assertThat(Files.exists(resources.resolve("META-INF/apionly/microcks"))).isFalse();
    }

    @Test
    void anOperationWhoseIdIsNotAJavaIdentifierIsRefused() throws Exception {
        Path openapi = directory.resolve("openapi.yaml");
        Path asyncapi = directory.resolve("asyncapi.yaml");
        Files.writeString(openapi, "openapi: 3.1.0\ninfo: {title: T, version: 1.0.0}\n");
        Files.writeString(asyncapi, """
                asyncapi: 3.0.0
                info: {title: T, version: 1.0.0}
                channels:
                  audit:
                    address: audit.v1
                    messages:
                      m: {payload: {x-fragment-path: asyncapi/M.yaml, type: string}}
                operations:
                  not-an-identifier:
                    action: send
                    channel: {$ref: '#/channels/audit'}
                    messages: [{$ref: '#/channels/audit/messages/m'}]
                """);
        assertThatThrownBy(() -> generate(openapi, asyncapi, "1.0.0"))
                .isInstanceOfAny(IllegalStateException.class, GenerationException.class)
                .hasMessageContaining("cannot be a Java identifier");
    }

    @Test
    void anOperationOnAChannelWithoutAnAddressIsRefused() throws Exception {
        Path openapi = directory.resolve("openapi.yaml");
        Path asyncapi = directory.resolve("asyncapi.yaml");
        Files.writeString(openapi, "openapi: 3.1.0\ninfo: {title: T, version: 1.0.0}\n");
        Files.writeString(asyncapi, """
                asyncapi: 3.0.0
                info: {title: T, version: 1.0.0}
                channels:
                  audit:
                    messages:
                      m: {payload: {x-fragment-path: asyncapi/M.yaml, type: string}}
                operations:
                  publishAudit:
                    action: send
                    channel: {$ref: '#/channels/audit'}
                    messages: [{$ref: '#/channels/audit/messages/m'}]
                """);
        assertThatThrownBy(() -> generate(openapi, asyncapi, "1.0.0"))
                .isInstanceOfAny(IllegalStateException.class, GenerationException.class)
                .hasMessageContaining("has no channel address");
    }
}
