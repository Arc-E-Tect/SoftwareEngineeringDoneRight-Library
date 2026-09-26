package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T15.2: independently of the REST Docs emitter's rendered tests, with every case's mapping
 * registered, every case's request sent with the JDK {@code HttpClient} is answered with the
 * case's status, its first declared content type, and the response class's valid body -- so the
 * emitter's own tests stay meaningful if the rendering changes.
 */
@DisplayName("T15.2 Replay without REST Docs")
class ReplayTest {

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void everyRequestIsAnsweredAsTheContractDeclares(Fixtures.Contract contract) throws Exception {
        GeneratedSuite suite = Suites.of(contract);
        try (DoubleServer server = new DoubleServer()) {
            suite.cases.forEach(c -> server.stub(suite.mapping(c)));
            for (GeneratedSuite.Case c : suite.cases) {
                Replay.Response response = Replay.send(server, Replay.of(suite.invalid(c)));
                JsonNode types = c.json().get("expectedContentTypes");
                assertThat(response.catchAll()).as(c.id()).isFalse();
                assertThat(response.status()).as(c.id()).isEqualTo(c.status());
                if (types.isEmpty()) {
                    assertThat(response.contentType()).as(c.id()).isNull();
                    assertThat(response.body()).as(c.id()).isEmpty();
                } else {
                    assertThat(response.contentType()).as(c.id()).isEqualTo(types.get(0).stringValue());
                    JsonNode bodyClass = c.json().get("responseBodyClass");
                    assertThat(response.body()).as(c.id()).isEqualTo(bodyClass.isNull() ? ""
                            : suite.type(suite.settings.basePackage() + "." + bodyClass.stringValue())
                            .getMethod("requiredBody").invoke(null));
                }
            }
        }
    }
}
