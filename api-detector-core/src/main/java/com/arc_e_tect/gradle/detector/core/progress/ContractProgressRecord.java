package com.arc_e_tect.gradle.detector.core.progress;

import com.arc_e_tect.gradle.detector.core.Described;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;

import java.time.Instant;

/**
 * Persisted, per-endpoint history of when an endpoint first reached each stage of its contract
 * lifecycle - declared, implemented, stubbed, verified - keyed by {@link EndpointFingerprint}
 * rather than by which plugin observed it, so that Shadow, Mirage, and Doppelganger API Detector
 * can all advance the same record from their own, partial view of the endpoint.
 *
 * @param fingerprint    the stable identifier computed by {@link EndpointFingerprint}
 * @param verb           the endpoint's current HTTP verb; not part of the key, refreshed on every
 *                       run that observes this endpoint
 * @param path           the endpoint's current path template; not part of the key, refreshed on
 *                       every run that observes this endpoint
 * @param declaringClass fully-qualified name of the {@code @RestController} class implementing
 *                       this endpoint, or {@code null} when no plugin has observed a real
 *                       implementation for it yet. Never set from stub evidence - see
 *                       {@link #stubbedAt()} for that.
 * @param declaredAt     when the endpoint was first observed as declared in the OpenAPI
 *                       documentation, or {@code null} if it never has been
 * @param implementedAt  when the endpoint was first observed as implemented by a real
 *                       {@code @RestController} method, or {@code null} if it never has been. A
 *                       WireMock stub mapping is never sufficient to set this field on its own -
 *                       see {@link #stubbedAt()} for that evidence instead.
 * @param stubbedAt      when the endpoint was first observed as backed by a WireMock stub mapping
 *                       file (Mirage API Detector's {@code scanMocks = true} mode), or
 *                       {@code null} if it never has been. Independent of {@link #implementedAt()}:
 *                       an endpoint can be stubbed without ever having a real implementation, and
 *                       vice versa.
 * @param verifiedAt     when the endpoint was first observed as verified by a contract
 *                       verification source, or {@code null} if it never has been
 * @param lastSeenAt     when the endpoint was last present in a run that could see it, or
 *                       {@code null} for a record that has never actually been seen
 * @param removedAt      when the endpoint was first observed missing from every set a run could
 *                       check it against, or {@code null} while it's still present
 */
public record ContractProgressRecord(
        String fingerprint,
        HttpVerb verb,
        String path,
        String declaringClass,
        Instant declaredAt,
        Instant implementedAt,
        Instant stubbedAt,
        Instant verifiedAt,
        Instant lastSeenAt,
        Instant removedAt) implements Described {
}
