package com.arc_e_tect.gradle.detector.core.progress;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.openapi.DescribedEndpoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ContractHistoryUpdater")
class ContractHistoryUpdaterTest {

    private final ContractHistoryUpdater updater = new ContractHistoryUpdater();
    private final EndpointFingerprint fingerprinter = new EndpointFingerprint();

    @Test
    @DisplayName("a newly declared endpoint gets declaredAt stamped with now")
    void newlyDeclaredEndpointGetsDeclaredAtStamped() {
        DescribedEndpoint endpoint = new DescribedEndpoint(HttpVerb.GET, "/orders/{id}", "getOrder", List.of());
        Instant now = Instant.parse("2026-01-10T09:00:00Z");

        Map<String, ContractProgressRecord> updated =
                updater.update(Map.of(), List.of(), List.of(endpoint), null, null, now);

        String fingerprint = fingerprinter.fingerprint(endpoint);
        assertThat(updated.get(fingerprint).declaredAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("a declared-only endpoint has no implementedAt")
    void declaredOnlyEndpointHasNoImplementedAt() {
        DescribedEndpoint endpoint = new DescribedEndpoint(HttpVerb.GET, "/orders/{id}", "getOrder", List.of());
        Instant now = Instant.parse("2026-01-10T09:00:00Z");

        Map<String, ContractProgressRecord> updated =
                updater.update(Map.of(), List.of(), List.of(endpoint), null, null, now);

        String fingerprint = fingerprinter.fingerprint(endpoint);
        assertThat(updated.get(fingerprint).implementedAt()).isNull();
    }

    @Test
    @DisplayName("an implemented endpoint gets its declaringClass set from the implementedNow pass")
    void implementedEndpointGetsDeclaringClassSet() {
        Endpoint endpoint = new Endpoint(HttpVerb.GET, "/orders/{id}", "com.acme.OrderController", "getOrder(Long)", "OrderController.java", 10);
        Instant now = Instant.parse("2026-01-20T14:30:00Z");

        Map<String, ContractProgressRecord> updated =
                updater.update(Map.of(), List.of(endpoint), List.of(), null, null, now);

        String fingerprint = fingerprinter.fingerprint(endpoint);
        assertThat(updated.get(fingerprint).declaringClass()).isEqualTo("com.acme.OrderController");
    }

    @Test
    @DisplayName("a plugin with no verification visibility (verifiedNow null) leaves an existing verifiedAt untouched")
    void noVerificationVisibilityLeavesVerifiedAtUntouched() {
        Endpoint endpoint = new Endpoint(HttpVerb.GET, "/orders/{id}", "com.acme.OrderController", "getOrder(Long)", "OrderController.java", 10);
        String fingerprint = fingerprinter.fingerprint(endpoint);
        Instant originalVerifiedAt = Instant.parse("2026-02-05T08:15:00Z");
        ContractProgressRecord existing = new ContractProgressRecord(
                fingerprint, HttpVerb.GET, "/orders/{id}", "com.acme.OrderController",
                Instant.parse("2026-01-10T09:00:00Z"), Instant.parse("2026-01-20T14:30:00Z"), null,
                originalVerifiedAt, originalVerifiedAt, null);
        Instant now = Instant.parse("2026-08-12T07:00:00Z");

        Map<String, ContractProgressRecord> updated = updater.update(
                Map.of(fingerprint, existing), List.of(endpoint), List.of(), null, null, now);

        assertThat(updated.get(fingerprint).verifiedAt()).isEqualTo(originalVerifiedAt);
    }

    @Test
    @DisplayName("a verification source's declaringClass never overwrites the real controller's declaringClass")
    void verificationSourceDoesNotOverwriteDeclaringClass() {
        Endpoint controllerEndpoint = new Endpoint(HttpVerb.GET, "/orders/{id}", "com.acme.OrderController", "getOrder(Long)", "OrderController.java", 10);
        String fingerprint = fingerprinter.fingerprint(controllerEndpoint);
        ContractProgressRecord existing = new ContractProgressRecord(
                fingerprint, HttpVerb.GET, "/orders/{id}", "com.acme.OrderController",
                Instant.parse("2026-01-10T09:00:00Z"), Instant.parse("2026-01-20T14:30:00Z"), null,
                null, Instant.parse("2026-01-20T14:30:00Z"), null);
        Endpoint testEndpoint = new Endpoint(HttpVerb.GET, "/orders/{id}", "com.acme.OrderControllerRestDocsTest", "getsOrder()", "OrderControllerRestDocsTest.java", 42);
        Instant now = Instant.parse("2026-02-05T08:15:00Z");

        Map<String, ContractProgressRecord> updated = updater.update(
                Map.of(fingerprint, existing), List.of(), List.of(), List.of(testEndpoint), null, now);

        assertThat(updated.get(fingerprint).declaringClass()).isEqualTo("com.acme.OrderController");
    }

    @Test
    @DisplayName("a stage timestamp that is already set is never overwritten by a later observation")
    void alreadySetTimestampIsNeverOverwritten() {
        Endpoint endpoint = new Endpoint(HttpVerb.GET, "/orders/{id}", "com.acme.OrderController", "getOrder(Long)", "OrderController.java", 10);
        String fingerprint = fingerprinter.fingerprint(endpoint);
        Instant originalImplementedAt = Instant.parse("2026-01-20T14:30:00Z");
        ContractProgressRecord existing = new ContractProgressRecord(
                fingerprint, HttpVerb.GET, "/orders/{id}", "com.acme.OrderController",
                null, originalImplementedAt, null, null, originalImplementedAt, null);
        Instant now = Instant.parse("2026-08-12T07:00:00Z");

        Map<String, ContractProgressRecord> updated = updater.update(
                Map.of(fingerprint, existing), List.of(endpoint), List.of(), null, null, now);

        assertThat(updated.get(fingerprint).implementedAt()).isEqualTo(originalImplementedAt);
    }

    @Test
    @DisplayName("an endpoint absent from every list passed is retained, not deleted")
    void absentEndpointIsRetainedNotDeleted() {
        String fingerprint = "aaaa000000000000";
        ContractProgressRecord existing = new ContractProgressRecord(
                fingerprint, HttpVerb.GET, "/gone", null,
                Instant.parse("2026-01-01T00:00:00Z"), null, null, null,
                Instant.parse("2026-01-01T00:00:00Z"), null);
        Instant now = Instant.parse("2026-08-12T07:00:00Z");

        Map<String, ContractProgressRecord> updated =
                updater.update(Map.of(fingerprint, existing), List.of(), List.of(), List.of(), null, now);

        assertThat(updated).containsKey(fingerprint);
    }

    @Test
    @DisplayName("an endpoint absent from every list passed gets removedAt stamped with now")
    void absentEndpointGetsRemovedAtStamped() {
        String fingerprint = "aaaa000000000000";
        ContractProgressRecord existing = new ContractProgressRecord(
                fingerprint, HttpVerb.GET, "/gone", null,
                Instant.parse("2026-01-01T00:00:00Z"), null, null, null,
                Instant.parse("2026-01-01T00:00:00Z"), null);
        Instant now = Instant.parse("2026-08-12T07:00:00Z");

        Map<String, ContractProgressRecord> updated =
                updater.update(Map.of(fingerprint, existing), List.of(), List.of(), List.of(), null, now);

        assertThat(updated.get(fingerprint).removedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("an endpoint absent only because verifiedNow is null (not because it's really gone) keeps removedAt unset")
    void absentOnlyBecauseVerifiedNowIsNullKeepsRemovedAtUnset() {
        Endpoint endpoint = new Endpoint(HttpVerb.GET, "/orders/{id}", "com.acme.OrderController", "getOrder(Long)", "OrderController.java", 10);
        String fingerprint = fingerprinter.fingerprint(endpoint);
        ContractProgressRecord existing = new ContractProgressRecord(
                fingerprint, HttpVerb.GET, "/orders/{id}", "com.acme.OrderController",
                null, Instant.parse("2026-01-20T14:30:00Z"), null, null,
                Instant.parse("2026-01-20T14:30:00Z"), null);
        Instant now = Instant.parse("2026-08-12T07:00:00Z");

        // implementedNow still reports the endpoint, so it's not really "gone" - only
        // verification visibility (verifiedNow) is absent (null) for this plugin's run.
        Map<String, ContractProgressRecord> updated = updater.update(
                Map.of(fingerprint, existing), List.of(endpoint), List.of(), null, null, now);

        assertThat(updated.get(fingerprint).removedAt()).isNull();
    }

    @Test
    @DisplayName("a removed-then-reappeared endpoint has its removedAt cleared")
    void removedThenReappearedEndpointHasRemovedAtCleared() {
        Endpoint endpoint = new Endpoint(HttpVerb.GET, "/orders/{id}", "com.acme.OrderController", "getOrder(Long)", "OrderController.java", 10);
        String fingerprint = fingerprinter.fingerprint(endpoint);
        ContractProgressRecord existing = new ContractProgressRecord(
                fingerprint, HttpVerb.GET, "/orders/{id}", "com.acme.OrderController",
                null, Instant.parse("2026-01-20T14:30:00Z"), null, null,
                Instant.parse("2026-01-20T14:30:00Z"), Instant.parse("2026-08-12T07:00:00Z"));
        Instant now = Instant.parse("2026-09-01T00:00:00Z");

        Map<String, ContractProgressRecord> updated = updater.update(
                Map.of(fingerprint, existing), List.of(endpoint), List.of(), null, null, now);

        assertThat(updated.get(fingerprint).removedAt()).isNull();
    }

    @Test
    @DisplayName("a removed-then-reappeared endpoint keeps its original implementedAt")
    void removedThenReappearedEndpointKeepsOriginalImplementedAt() {
        Endpoint endpoint = new Endpoint(HttpVerb.GET, "/orders/{id}", "com.acme.OrderController", "getOrder(Long)", "OrderController.java", 10);
        String fingerprint = fingerprinter.fingerprint(endpoint);
        Instant originalImplementedAt = Instant.parse("2026-01-20T14:30:00Z");
        ContractProgressRecord existing = new ContractProgressRecord(
                fingerprint, HttpVerb.GET, "/orders/{id}", "com.acme.OrderController",
                null, originalImplementedAt, null, null,
                originalImplementedAt, Instant.parse("2026-08-12T07:00:00Z"));
        Instant now = Instant.parse("2026-09-01T00:00:00Z");

        Map<String, ContractProgressRecord> updated = updater.update(
                Map.of(fingerprint, existing), List.of(endpoint), List.of(), null, null, now);

        assertThat(updated.get(fingerprint).implementedAt()).isEqualTo(originalImplementedAt);
    }

    @Test
    @DisplayName("a record already marked removed is not re-stamped with a later removedAt")
    void alreadyRemovedRecordKeepsItsOriginalRemovedAt() {
        String fingerprint = "aaaa000000000000";
        Instant originalRemovedAt = Instant.parse("2026-02-01T00:00:00Z");
        ContractProgressRecord existing = new ContractProgressRecord(
                fingerprint, HttpVerb.GET, "/gone", null,
                Instant.parse("2026-01-01T00:00:00Z"), null, null, null,
                Instant.parse("2026-01-01T00:00:00Z"), originalRemovedAt);
        Instant now = Instant.parse("2026-08-12T07:00:00Z");

        Map<String, ContractProgressRecord> updated =
                updater.update(Map.of(fingerprint, existing), List.of(), List.of(), List.of(), null, now);

        assertThat(updated.get(fingerprint).removedAt()).isEqualTo(originalRemovedAt);
    }

    @Test
    @DisplayName("refreshes lastSeenAt to now for every endpoint present in any of the current run's lists")
    void refreshesLastSeenAtToNow() {
        DescribedEndpoint endpoint = new DescribedEndpoint(HttpVerb.GET, "/orders/{id}", "getOrder", List.of());
        Instant now = Instant.parse("2026-05-01T00:00:00Z");

        Map<String, ContractProgressRecord> updated =
                updater.update(Map.of(), List.of(), List.of(endpoint), null, null, now);

        String fingerprint = fingerprinter.fingerprint(endpoint);
        assertThat(updated.get(fingerprint).lastSeenAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("a stub-only endpoint gets stubbedAt stamped but implementedAt stays null")
    void stubOnlyEndpointGetsStubbedAtStampedButNotImplementedAt() {
        Endpoint endpoint = new Endpoint(HttpVerb.GET, "/orders/{id}", "(mappings)", "getOrder", "getOrder.json", 3);
        Instant now = Instant.parse("2026-01-20T14:30:00Z");

        Map<String, ContractProgressRecord> updated =
                updater.update(Map.of(), List.of(), List.of(), null, List.of(endpoint), now);

        String fingerprint = fingerprinter.fingerprint(endpoint);
        assertThat(updated.get(fingerprint).stubbedAt()).isEqualTo(now);
        assertThat(updated.get(fingerprint).implementedAt()).isNull();
    }

    @Test
    @DisplayName("a real implementation does not set stubbedAt")
    void realImplementationDoesNotSetStubbedAt() {
        Endpoint endpoint = new Endpoint(HttpVerb.GET, "/orders/{id}", "com.acme.OrderController", "getOrder(Long)", "OrderController.java", 10);
        Instant now = Instant.parse("2026-01-20T14:30:00Z");

        Map<String, ContractProgressRecord> updated =
                updater.update(Map.of(), List.of(endpoint), List.of(), null, null, now);

        String fingerprint = fingerprinter.fingerprint(endpoint);
        assertThat(updated.get(fingerprint).stubbedAt()).isNull();
    }

    @Test
    @DisplayName("stubbedAt does not overwrite an existing declaringClass set by a real implementation")
    void stubbedNowDoesNotOverwriteDeclaringClass() {
        Endpoint controllerEndpoint = new Endpoint(HttpVerb.GET, "/orders/{id}", "com.acme.OrderController", "getOrder(Long)", "OrderController.java", 10);
        String fingerprint = fingerprinter.fingerprint(controllerEndpoint);
        ContractProgressRecord existing = new ContractProgressRecord(
                fingerprint, HttpVerb.GET, "/orders/{id}", "com.acme.OrderController",
                Instant.parse("2026-01-10T09:00:00Z"), Instant.parse("2026-01-20T14:30:00Z"), null,
                null, Instant.parse("2026-01-20T14:30:00Z"), null);
        Endpoint stubEndpoint = new Endpoint(HttpVerb.GET, "/orders/{id}", "(mappings)", "getOrder", "getOrder.json", 3);
        Instant now = Instant.parse("2026-02-05T08:15:00Z");

        Map<String, ContractProgressRecord> updated = updater.update(
                Map.of(fingerprint, existing), List.of(), List.of(), null, List.of(stubEndpoint), now);

        assertThat(updated.get(fingerprint).declaringClass()).isEqualTo("com.acme.OrderController");
    }

    @Test
    @DisplayName("an endpoint present only via stubbedNow is not marked removed")
    void endpointPresentOnlyViaStubbedNowIsNotMarkedRemoved() {
        Endpoint endpoint = new Endpoint(HttpVerb.GET, "/orders/{id}", "(mappings)", "getOrder", "getOrder.json", 3);
        String fingerprint = fingerprinter.fingerprint(endpoint);
        ContractProgressRecord existing = new ContractProgressRecord(
                fingerprint, HttpVerb.GET, "/orders/{id}", null,
                null, null, Instant.parse("2026-01-01T00:00:00Z"), null,
                Instant.parse("2026-01-01T00:00:00Z"), null);
        Instant now = Instant.parse("2026-08-12T07:00:00Z");

        Map<String, ContractProgressRecord> updated = updater.update(
                Map.of(fingerprint, existing), List.of(), List.of(), null, List.of(endpoint), now);

        assertThat(updated.get(fingerprint).removedAt()).isNull();
    }

    @Test
    @DisplayName("a plugin with no stub visibility (stubbedNow null) leaves an existing stubbedAt untouched")
    void noStubVisibilityLeavesStubbedAtUntouched() {
        Endpoint endpoint = new Endpoint(HttpVerb.GET, "/orders/{id}", "com.acme.OrderController", "getOrder(Long)", "OrderController.java", 10);
        String fingerprint = fingerprinter.fingerprint(endpoint);
        Instant originalStubbedAt = Instant.parse("2026-01-05T00:00:00Z");
        ContractProgressRecord existing = new ContractProgressRecord(
                fingerprint, HttpVerb.GET, "/orders/{id}", null,
                Instant.parse("2026-01-01T00:00:00Z"), null, originalStubbedAt, null,
                originalStubbedAt, null);
        Instant now = Instant.parse("2026-08-12T07:00:00Z");

        Map<String, ContractProgressRecord> updated = updater.update(
                Map.of(fingerprint, existing), List.of(endpoint), List.of(), null, null, now);

        assertThat(updated.get(fingerprint).stubbedAt()).isEqualTo(originalStubbedAt);
    }
}
