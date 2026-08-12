package com.arc_e_tect.gradle.detector.core.progress;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.openapi.DescribedEndpoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EndpointFingerprint")
class EndpointFingerprintTest {

    private final EndpointFingerprint fingerprinter = new EndpointFingerprint();

    @Test
    @DisplayName("produces a 16 hex character fingerprint")
    void producesA16HexCharacterFingerprint() {
        Endpoint endpoint = new Endpoint(HttpVerb.GET, "/orders/{id}", "com.acme.OrderController", "getOrder(Long)", "OrderController.java", 10);

        assertThat(fingerprinter.fingerprint(endpoint)).matches("[0-9a-f]{16}");
    }

    @Test
    @DisplayName("is stable across repeated calls with the same verb and path")
    void isStableAcrossRepeatedCalls() {
        Endpoint first = new Endpoint(HttpVerb.GET, "/orders/{id}", "com.acme.A", "a()", "A.java", 1);
        Endpoint second = new Endpoint(HttpVerb.GET, "/orders/{id}", "com.acme.B", "b()", "B.java", 99);

        assertThat(fingerprinter.fingerprint(first)).isEqualTo(fingerprinter.fingerprint(second));
    }

    @Test
    @DisplayName("produces the same fingerprint for an Endpoint and a DescribedEndpoint sharing the same verb and path")
    void producesSameFingerprintForEndpointAndDescribedEndpoint() {
        Endpoint endpoint = new Endpoint(HttpVerb.GET, "/orders/{id}", "com.acme.OrderController", "getOrder(Long)", "OrderController.java", 10);
        DescribedEndpoint described = new DescribedEndpoint(HttpVerb.GET, "/orders/{id}", "getOrder", List.of());

        assertThat(fingerprinter.fingerprint(endpoint)).isEqualTo(fingerprinter.fingerprint(described));
    }

    @Test
    @DisplayName("produces different fingerprints for different paths")
    void producesDifferentFingerprintsForDifferentPaths() {
        Endpoint first = new Endpoint(HttpVerb.GET, "/orders/{id}", "com.acme.A", "a()", "A.java", 1);
        Endpoint second = new Endpoint(HttpVerb.GET, "/invoices/{id}", "com.acme.A", "a()", "A.java", 1);

        assertThat(fingerprinter.fingerprint(first)).isNotEqualTo(fingerprinter.fingerprint(second));
    }

    @Test
    @DisplayName("produces different fingerprints for different verbs")
    void producesDifferentFingerprintsForDifferentVerbs() {
        Endpoint get = new Endpoint(HttpVerb.GET, "/orders/{id}", "com.acme.A", "a()", "A.java", 1);
        Endpoint delete = new Endpoint(HttpVerb.DELETE, "/orders/{id}", "com.acme.A", "a()", "A.java", 1);

        assertThat(fingerprinter.fingerprint(get)).isNotEqualTo(fingerprinter.fingerprint(delete));
    }
}
