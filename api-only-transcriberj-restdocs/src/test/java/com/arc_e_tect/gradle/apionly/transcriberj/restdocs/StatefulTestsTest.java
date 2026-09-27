package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T20.4: with a correct fixture, the stateful tests are idempotent and independent. Against the
 * stateful server, the suite runs twice without the store being reset, then three times in a random
 * method and class order, each seed printed, and every test passes every time. With the fixture
 * broken -- {@code arrangeState} doing nothing -- the success and not-found tests fail, with the
 * fixture messages.
 */
@DisplayName("T20.4 Stateful tests are idempotent and independent")
class StatefulTestsTest {

    static List<Fixtures.Contract> contracts() {
        return List.of(Fixtures.KINDS, Fixtures.USER_ACCOUNT);
    }

    /** JUnit's configuration for a random method and class order, from a seed. */
    static Map<String, String> randomOrder(long seed) {
        return Map.of("junit.jupiter.testmethod.order.default", "org.junit.jupiter.api.MethodOrderer$Random",
                "junit.jupiter.testclass.order.default", "org.junit.jupiter.api.ClassOrderer$Random",
                "junit.jupiter.execution.order.random.seed", Long.toString(seed));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void everyTestPassesAgainAndInAnyOrder(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        ValidatingServer behaviour = ValidatingServer.of(suite);
        try (ContractServer server = new ContractServer(behaviour)) {
            for (int i = 1; i <= 2; i++) {
                GeneratedSuite.Run run = suite.run(server);
                assertThat(run.failed()).as("run %d, the store not reset", i)
                        .extracting(o -> o.method() + ": " + o.message()).isEmpty();
            }
            assertThat(behaviour.store.size()).as("the store after two runs").isPositive();
            List<String> previous = null;
            Random seeds = new Random();
            for (int i = 1; i <= 3; i++) {
                long seed = seeds.nextLong();
                System.out.printf("T20.4 %s: random order run %d, seed %d%n", contract.name(), i, seed);
                GeneratedSuite.Run run = suite.run(server, null, randomOrder(seed));
                assertThat(run.failed()).as("random order run %d, seed %d", i, seed)
                        .extracting(o -> o.method() + ": " + o.message()).isEmpty();
                assertThat(run.outcomes()).hasSize(suite.cases.size());
                if (previous != null && suite.cases.size() > 3) {
                    assertThat(run.order()).as("the order, seed %d", seed).isNotEqualTo(previous);
                }
                previous = run.order();
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void aBrokenFixtureFailsTheStatefulTestsWithTheFixtureMessages(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        ValidatingServer behaviour = ValidatingServer.of(suite);
        for (GeneratedSuite.Case c : suite.cases) {
            if (c.requiresState()) behaviour.store.misarrange(new FixtureHookTest.JsonCase(c).stateful());
        }
        GeneratedSuite.Run run;
        try (ContractServer server = new ContractServer(behaviour)) {
            run = suite.run(server, x -> {
            }, Map.of());
        }

        assertThat(run.failed()).isNotEmpty();
        for (GeneratedSuite.Outcome failed : run.failed()) {
            GeneratedSuite.Case c = suite.cases.stream().filter(k -> k.key(true).equals(
                    failed.testClass() + "#" + failed.method())).findFirst().orElseThrow();
            assertThat(c.requiresState()).as(c.id()).isTrue();
            assertThat(failed.message()).as(c.id()).endsWith(Messages.CHECK_FIXTURE);
        }
        assertThat(run.failed().stream().map(o -> suite.cases.stream()
                .filter(k -> k.key(true).equals(o.testClass() + "#" + o.method())).findFirst().orElseThrow().kind())
                .distinct()).containsExactlyInAnyOrder("SUCCESS", "NOT_FOUND");
    }
}
