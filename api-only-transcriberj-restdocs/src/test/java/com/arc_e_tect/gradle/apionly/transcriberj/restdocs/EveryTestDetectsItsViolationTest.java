package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T14.2: for every case of the user-account contract and of the keyword corpus -- every one, not
 * a sample -- the whole generated suite runs against a server that stops enforcing exactly that
 * case's keyword at exactly that case's location, and answers {@code 200} there instead. Exactly
 * one test fails, the one for that case, saying the constraint is not enforced. This is what
 * proves each test detects the violation it claims to.
 */
@DisplayName("T14.2 Every test detects its own violation")
class EveryTestDetectsItsViolationTest {

    @TestFactory
    Stream<DynamicContainer> exactlyTheTestOfTheUnenforcedConstraintFails() {
        return Stream.of(Fixtures.USER_ACCOUNT, Fixtures.KEYWORDS).map(contract ->
                DynamicContainer.dynamicContainer(contract.name(), Stream.of(contract).flatMap(c -> {
                    GeneratedSuite suite = Suites.of(c);
                    ContractServer server = new ContractServer(ValidatingServer.of(suite));
                    List<GeneratedSuite.Case> cases = suite.cases;
                    return Stream.concat(cases.stream().map(target -> DynamicTest.dynamicTest(target.id(), () -> {
                        server.behave(ValidatingServer.ignoring(suite, target.json()));
                        GeneratedSuite.Run run = suite.run(server);

                        assertThat(run.outcomes()).hasSize(cases.size());
                        assertThat(run.failed()).as("the failed tests, with their messages: %s", run.failed().stream()
                                        .map(o -> o.method() + ": " + o.message()).toList())
                                .extracting(o -> o.testClass() + "#" + o.method())
                                .containsExactly(target.key(true));
                        assertThat(run.failed().get(0).message()).isEqualTo(Messages.accepted(target.json(), 200));
                    })), Stream.of(DynamicTest.dynamicTest("stop the server", server::close)));
                })));
    }
}
