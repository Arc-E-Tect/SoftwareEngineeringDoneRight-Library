package com.arc_e_tect.gradle.detector.core.progress;

import com.arc_e_tect.gradle.detector.core.Described;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * Computes a stable identifier for an endpoint from its contract identity alone - HTTP verb and
 * path - ignoring the class/method that happens to implement it or the line it's declared on.
 *
 * <p>Works for any {@link Described} value, so it fingerprints a controller-scanned
 * {@link com.arc_e_tect.gradle.detector.core.model.Endpoint} and an OpenAPI-derived
 * {@link com.arc_e_tect.gradle.detector.core.openapi.DescribedEndpoint} identically whenever they
 * share the same verb and path - exactly what
 * {@link com.arc_e_tect.gradle.detector.core.progress.ContractHistoryUpdater} needs to recognise
 * the same endpoint across every plugin's own, differently-shaped view of it.</p>
 */
public class EndpointFingerprint {

    private static final int FINGERPRINT_LENGTH = 16;

    /** Creates a new {@code EndpointFingerprint}. */
    public EndpointFingerprint() {}

    /**
     * Computes the fingerprint for an endpoint's verb + path.
     *
     * @param endpoint the endpoint to fingerprint
     * @return the first 16 hex characters of the SHA-256 hash of the normalized verb + path
     */
    public String fingerprint(Described endpoint) {
        String normalized = endpoint.verb().name() + " " + endpoint.path().trim();
        return sha256Hex(normalized).substring(0, FINGERPRINT_LENGTH);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format(Locale.ROOT, "%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 message digest not available", e);
        }
    }
}
