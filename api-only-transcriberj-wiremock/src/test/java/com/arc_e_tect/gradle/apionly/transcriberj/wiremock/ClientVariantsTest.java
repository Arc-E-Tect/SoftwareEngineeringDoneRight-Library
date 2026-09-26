package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T15.16: the same mappings, registered through the static {@code WireMock.stubFor(...)} client and
 * through a {@code WireMock} client instance's {@code register(...)} pointing at a second in-process
 * server, pass T15.1. In WireMock 3.13.2, an instance takes a mapping through {@code register}; it
 * has no {@code stubFor}.
 */
@DisplayName("T15.16 The client variants")
class ClientVariantsTest {

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void theStaticClientRegistersThem(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        GeneratedSuite.Run run;
        try (DoubleServer server = new DoubleServer()) {
            run = suite.run(server, true, Harness.Client.STATIC, false);
        }
        EverythingPassesTest.assertServedByOwnStubs(suite, run);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void aClientInstanceForASecondServerRegistersThem(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        GeneratedSuite.Run run;
        try (DoubleServer first = new DoubleServer(); DoubleServer second = new DoubleServer()) {
            run = suite.run(second, true, Harness.Client.INSTANCE, false);
            EverythingPassesTest.assertServedByOwnStubs(suite, run);
            assertThat(first.journal()).isEmpty();
        }
    }
}
