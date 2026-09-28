package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T14.7 and T20.3: {@code arrangeState} is called exactly once before each case that requires state,
 * and {@code arrangeStatelessCase} exactly once before each that does not, each with its case,
 * before the request is sent. A class that overrides neither compiles; its stateful tests fail with
 * {@code FixtureNotImplementedException}, whose message names the case and suggests an
 * implementation, and its stateless tests run as ever. The suggestion, pasted into the class,
 * compiles with every lint on and turns each such failure into the fixture's own.
 */
@DisplayName("T14.7, T20.3 The fixture hooks")
class FixtureHookTest {

    static List<Fixtures.Contract> contracts() {
        return List.of(Fixtures.USER_ACCOUNT, Fixtures.KINDS, Fixtures.TRANSMISSION);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void eachHookIsCalledOnceBeforeItsKindOfCaseAndBeforeTheRequest(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        GeneratedSuite.Run run;
        try (ContractServer server = new ContractServer(ValidatingServer.of(suite))) {
            run = suite.run(server);
        }

        assertThat(run.failed()).isEmpty();
        assertThat(run.arrangements()).hasSize(suite.cases.size());
        assertThat(run.received()).hasSize(suite.cases.size());
        for (GeneratedSuite.Case c : suite.cases) {
            List<Harness.Arrangement> calls = run.arrangements().stream()
                    .filter(a -> a.testClass().equals(c.tests() + GeneratedSuite.RECORDING) && a.caseId().equals(c.id()))
                    .toList();
            assertThat(calls).as(c.id()).hasSize(1);
            assertThat(calls.get(0).hook()).as(c.id())
                    .isEqualTo(c.requiresState() ? Harness.STATE : Harness.STATELESS);
        }
        assertThat(run.arrangements()).extracting(Harness.Arrangement::hook)
                .contains(Harness.STATE, Harness.STATELESS);
        // Tests run one at a time: every hook call is followed by exactly one request before the next call.
        List<Long> hooks = run.arrangements().stream().map(Harness.Arrangement::sequence).sorted().toList();
        List<ContractServer.Received> requests = run.received().stream()
                .sorted(Comparator.comparingLong(ContractServer.Received::sequence)).toList();
        for (int i = 0; i < hooks.size(); i++) {
            long next = i + 1 < hooks.size() ? hooks.get(i + 1) : Long.MAX_VALUE;
            assertThat(requests.get(i).sequence()).isGreaterThan(hooks.get(i)).isLessThan(next);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void withoutAFixtureEveryStatefulTestFailsNamingItsCaseAndEveryOtherRuns(Fixtures.Contract contract)
            throws IOException {
        GeneratedSuite suite = Suites.of(contract);
        GeneratedSuite.Run run;
        try (ContractServer server = new ContractServer(ValidatingServer.of(suite))) {
            run = suite.run(server, false, null);
        }

        assertThat(run.outcomes()).hasSize(suite.cases.size());
        assertThat(run.arrangements()).isEmpty();
        for (GeneratedSuite.Case c : suite.cases) {
            GeneratedSuite.Outcome outcome = run.outcomes().get(c.key(false));
            if (!c.requiresState()) {
                assertThat(outcome.failure()).as(c.id()).isNull();
                continue;
            }
            Throwable failure = outcome.failure();
            assertThat(failure).as(c.id()).isNotNull();
            assertThat(failure.getClass().getSimpleName()).isEqualTo(ContractTests.EXCEPTION);
            // Reported as a failure, not as aborted: it is an assertion failure, not a TestAbortedException.
            assertThat(failure).isInstanceOf(org.opentest4j.AssertionFailedError.class)
                    .isNotInstanceOf(org.opentest4j.TestAbortedException.class);
            JsonCase json = new JsonCase(c);
            assertThat(failure.getMessage()).as(c.id())
                    .startsWith("Case " + c.id() + " of " + json.method() + " " + json.path() + " (" + c.kind()
                            + ", expects " + json.status() + (json.variant() == null ? "" : ", variant " + json.variant())
                            + ", path values " + json.pathValues() + ") needs the implementation under test in a "
                            + "particular state, but arrangeState is not implemented yet.")
                    .contains("Implement arrangeState in the class that implements " + c.tests() + ". It starts as:\n\n"
                            + "@Override\npublic void arrangeState(ContractCase contractCase) {\n")
                    .contains("case \"" + c.id() + "\" -> {");
        }
        for (String tests : suite.interfaces) {
            assertThat(Files.readString(suite.source("com/example/contract/restdocs/" + tests + ".java")))
                    .contains("    default void arrangeStatelessCase(ContractCase contractCase) {\n    }\n")
                    // The stateless tests' limit, as the TranscriberJ's README states it: read the first
                    // failure first and re-run on fresh state, rather than reset between runs.
                    .contains(" * <p><b>WARNING:</b> these tests are not independent of each other.")
                    .contains("Read the first failing stateless test first")
                    .doesNotContain("reset the state between runs");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void theSuggestionCompilesAndTurnsEveryFailureIntoTheFixturesOwn(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        Map<String, String> suggestions = new HashMap<>();
        try (ContractServer server = new ContractServer(ValidatingServer.of(suite))) {
            GeneratedSuite.Run plain = suite.run(server, false, null);
            for (GeneratedSuite.Case c : suite.cases) {
                if (!c.requiresState()) continue;
                String message = plain.outcomes().get(c.key(false)).message();
                suggestions.put(c.tests(), message.substring(message.indexOf("It starts as:\n\n") + 15));
            }
        }
        assertThat(suggestions.keySet()).containsExactlyInAnyOrderElementsOf(suite.stateful);

        List<Diagnostic<? extends JavaFileObject>> diagnostics = new ArrayList<>();
        List<Class<?>> suggested = suite.variant("Suggested", suggestions::get, diagnostics);
        assertThat(diagnostics).filteredOn(d -> d.getKind() != Diagnostic.Kind.NOTE).extracting(Object::toString)
                .isEmpty();

        ValidatingServer behaviour = ValidatingServer.of(suite);
        // The store as a run of another test class might leave it: every stateful case's state wrong.
        for (GeneratedSuite.Case c : suite.cases) {
            if (c.requiresState()) behaviour.store.misarrange(new JsonCase(c).stateful());
        }
        GeneratedSuite.Run run;
        try (ContractServer server = new ContractServer(behaviour)) {
            run = suite.run(server, new GeneratedSuite.Options(suggested, null, x -> {
            }, Map.of()));
        }
        Set<String> kindsFailing = new java.util.TreeSet<>();
        for (GeneratedSuite.Case c : suite.cases) {
            GeneratedSuite.Outcome outcome = run.outcomes().get(c.key("Suggested"));
            if (!c.requiresState()) {
                assertThat(outcome.failure()).as(c.id()).isNull();
                continue;
            }
            if (outcome.passed()) continue;
            assertThat(outcome.failure().getClass().getSimpleName()).as(c.id()).isNotEqualTo(ContractTests.EXCEPTION);
            assertThat(outcome.message()).as(c.id()).endsWith(Messages.CHECK_FIXTURE);
            kindsFailing.add(c.kind());
        }
        if (contract == Fixtures.KINDS) assertThat(kindsFailing).containsExactly("NOT_FOUND", "SUCCESS");
    }

    @Test
    void aClassThatArrangesStateNeverSeesTheException() {
        GeneratedSuite suite = Suites.of(Fixtures.KINDS);
        GeneratedSuite.Run run;
        try (ContractServer server = new ContractServer(ValidatingServer.of(suite))) {
            run = suite.run(server);
        }
        assertThat(run.outcomes().values()).allSatisfy(o -> assertThat(o.failure()).isNull());
    }

    /** A case's request, as the report records it, and the state it needs as the harness's fixture sees it. */
    record JsonCase(GeneratedSuite.Case c) {

        String method() {
            return c.json().get("request").get("method").stringValue();
        }

        String path() {
            return c.json().get("request").get("pathTemplate").stringValue();
        }

        int status() {
            return c.json().get("expectedStatus").asInt();
        }

        String variant() {
            return c.json().get("variant").isNull() ? null : c.json().get("variant").stringValue();
        }

        List<String> pathValues() {
            List<String> out = new ArrayList<>();
            c.json().get("request").get("pathParameters").forEach(v -> out.add(v.stringValue()));
            return out;
        }

        Harness.Stateful stateful() {
            return new Harness.Stateful(c.kind(), status(), path(), pathValues());
        }
    }
}
