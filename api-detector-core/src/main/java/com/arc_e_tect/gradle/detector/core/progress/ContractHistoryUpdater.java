package com.arc_e_tect.gradle.detector.core.progress;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.openapi.DescribedEndpoint;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Produces the next {@link ContractProgressRecord} map to persist, from the previously persisted
 * map and whichever of the current run's declared/implemented/verified endpoint sets the calling
 * plugin has in hand.
 *
 * <p>Every endpoint's contract lifecycle has three stages - declared, implemented, verified - but
 * no single plugin ever observes all three: Shadow and Mirage API Detector only ever see
 * {@code implementedNow}/{@code declaredNow}, never verification evidence; Doppelganger API
 * Detector sees all three. Whichever of {@code declaredAt}/{@code implementedAt}/{@code verifiedAt}
 * matches a stage this call actually has evidence for is stamped with {@code now} the first time
 * that stage is observed, and is never overwritten afterwards - so a stage a plugin has no
 * visibility into (passed as {@code null}, or simply absent from the list it did pass) is left
 * completely untouched, letting a <em>different</em> plugin's later run fill it in instead. Every
 * previously persisted record whose fingerprint appears in none of the lists passed this call is
 * kept, unchanged, with {@code removedAt} stamped the first time it goes missing. Records are
 * never deleted.</p>
 */
public class ContractHistoryUpdater {

    private final EndpointFingerprint fingerprinter = new EndpointFingerprint();

    /** Creates a new {@code ContractHistoryUpdater}. */
    public ContractHistoryUpdater() {}

    /**
     * Computes the updated history map to persist.
     *
     * @param existing      the previously persisted history, keyed by fingerprint; empty on a
     *                      first run
     * @param implementedNow the current run's implemented endpoints (e.g. from scanning
     *                      {@code @RestController} classes)
     * @param declaredNow   the current run's declared endpoints (from the OpenAPI documentation)
     * @param verifiedNow   the current run's verified endpoints, or {@code null} when the calling
     *                      plugin has no verification evidence to offer at all - as opposed to an
     *                      empty list, which means it looked and found none
     * @param now           the instant to stamp newly-reached stages and newly-observed removals with
     * @return the updated history map, keyed by fingerprint
     */
    public Map<String, ContractProgressRecord> update(
            Map<String, ContractProgressRecord> existing,
            List<Endpoint> implementedNow,
            List<DescribedEndpoint> declaredNow,
            List<Endpoint> verifiedNow,
            Instant now) {
        Map<String, ContractProgressRecord> updated = new LinkedHashMap<>(existing);
        Set<String> seen = new HashSet<>();

        for (Endpoint endpoint : implementedNow) {
            String fingerprint = fingerprinter.fingerprint(endpoint);
            seen.add(fingerprint);
            ContractProgressRecord record = updated.getOrDefault(fingerprint, blank(fingerprint));
            Instant implementedAt = record.implementedAt() != null ? record.implementedAt() : now;
            updated.put(fingerprint, new ContractProgressRecord(
                    fingerprint, endpoint.verb(), endpoint.path(), endpoint.declaringClass(),
                    record.declaredAt(), implementedAt, record.verifiedAt(), now, null));
        }

        for (DescribedEndpoint endpoint : declaredNow) {
            String fingerprint = fingerprinter.fingerprint(endpoint);
            seen.add(fingerprint);
            ContractProgressRecord record = updated.getOrDefault(fingerprint, blank(fingerprint));
            Instant declaredAt = record.declaredAt() != null ? record.declaredAt() : now;
            updated.put(fingerprint, new ContractProgressRecord(
                    fingerprint, endpoint.verb(), endpoint.path(), record.declaringClass(),
                    declaredAt, record.implementedAt(), record.verifiedAt(), now, null));
        }

        if (verifiedNow != null) {
            for (Endpoint endpoint : verifiedNow) {
                String fingerprint = fingerprinter.fingerprint(endpoint);
                seen.add(fingerprint);
                ContractProgressRecord record = updated.getOrDefault(fingerprint, blank(fingerprint));
                Instant verifiedAt = record.verifiedAt() != null ? record.verifiedAt() : now;
                // declaringClass deliberately left untouched here: a verification source's Endpoint
                // identifies the test (or contract file) that supplied the evidence, not the
                // production @RestController - only the implementedNow pass above may set it.
                updated.put(fingerprint, new ContractProgressRecord(
                        fingerprint, endpoint.verb(), endpoint.path(), record.declaringClass(),
                        record.declaredAt(), record.implementedAt(), verifiedAt, now, null));
            }
        }

        for (Map.Entry<String, ContractProgressRecord> entry : existing.entrySet()) {
            if (!seen.contains(entry.getKey())) {
                updated.put(entry.getKey(), markRemoved(entry.getValue(), now));
            }
        }

        return updated;
    }

    private ContractProgressRecord blank(String fingerprint) {
        return new ContractProgressRecord(fingerprint, null, null, null, null, null, null, null, null);
    }

    private ContractProgressRecord markRemoved(ContractProgressRecord record, Instant now) {
        if (record.removedAt() != null) {
            return record;
        }
        return new ContractProgressRecord(
                record.fingerprint(), record.verb(), record.path(), record.declaringClass(),
                record.declaredAt(), record.implementedAt(), record.verifiedAt(),
                record.lastSeenAt(), now);
    }
}
