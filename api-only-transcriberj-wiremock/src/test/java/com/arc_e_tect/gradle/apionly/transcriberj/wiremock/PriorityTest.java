package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.request;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * T15.9: a project's stub for the same method and path at WireMock's default priority -- as a
 * hand-written happy-path stub would be, answering {@code 202} -- never wins over a generated stub
 * for a case, even registered after it; and the {@code priority} option sets the priority.
 */
@DisplayName("T15.9 Priority")
class PriorityTest {

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void aProjectStubAtTheDefaultPriorityNeverWins(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        try (DoubleServer server = new DoubleServer()) {
            for (GeneratedSuite.Case c : suite.cases) {
                server.reset();
                Replay.Request sent = Replay.of(suite.invalid(c));
                server.stub(suite.mapping(c));
                server.stub(request(sent.method(), urlPathEqualTo(Replay.path(sent)))
                        .willReturn(aResponse().withStatus(202)));
                Replay.Response response = Replay.send(server, sent);
                assertThat(response.status()).as(c.id()).isEqualTo(c.status());
                assertThat(server.journal().get(0).servedBy()).as(c.id()).isEqualTo(suite.mappingName(c));
            }
        }
    }

    @Test
    void theDefaultPriorityIsWireMocksHighest() throws Exception {
        GeneratedSuite suite = Suites.of(Fixtures.RESPONSES);
        assertThat(suite.stubs().getField("PRIORITY").get(null)).isEqualTo(1);
        assertThat(suite.mapping(suite.cases.get(0)).build().getPriority()).isEqualTo(1);
    }

    @Test
    void theOptionSetsThePriority() throws Exception {
        GeneratedSuite suite = GeneratedSuite.of(Fixtures.RESPONSES, Map.of("priority", " 3 "),
                Suites.directory("priority"));
        assertThat(suite.stubs().getField("PRIORITY").get(null)).isEqualTo(3);
        for (GeneratedSuite.Case c : suite.cases) {
            assertThat(suite.mapping(c).build().getPriority()).isEqualTo(3);
        }
    }
}
