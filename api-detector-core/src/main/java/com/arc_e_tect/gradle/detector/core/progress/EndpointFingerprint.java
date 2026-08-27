package com.arc_e_tect.gradle.detector.core.progress;

import com.arc_e_tect.gradle.detector.core.Described;
import com.arc_e_tect.gradle.detector.core.model.PathTemplates;

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
 *
 * <p>Every {@code "/"}-delimited path-variable placeholder segment - i.e. one
 * {@link PathTemplates#isPlaceholder(String)} recognises, such as {@code "{id}"} or
 * {@code "{customerId}"} - is canonicalised to the same fixed token before hashing, so two
 * sources naming the same path variable differently still fingerprint identically. This matches
 * {@link com.arc_e_tect.gradle.detector.core.model.PathMatcher}'s own documented tolerance of
 * path-variable naming differences, which this class previously fell short of: without it, a
 * WireMock stub matched by a regular expression - rewritten into a placeholder segment by
 * {@code WireMockStubScanner} without knowing the corresponding OpenAPI/controller variable's real
 * name - would never fingerprint the same as the endpoint it actually stubs.</p>
 */
public class EndpointFingerprint {

    private static final int FINGERPRINT_LENGTH = 16;
    private static final String PLACEHOLDER_TOKEN = "{}";

    /** Creates a new {@code EndpointFingerprint}. */
    public EndpointFingerprint() {}

    /**
     * Computes the fingerprint for an endpoint's verb + path.
     *
     * @param endpoint the endpoint to fingerprint
     * @return the first 16 hex characters of the SHA-256 hash of the normalized verb + canonical
     *         path
     */
    public String fingerprint(Described endpoint) {
        String normalized = endpoint.verb().name() + " " + canonicalize(endpoint.path().trim());
        return sha256Hex(normalized).substring(0, FINGERPRINT_LENGTH);
    }

    /**
     * Replaces every placeholder segment in {@code path} with the same fixed token, so that only
     * a path's literal segments and their positions - never a path variable's chosen name -
     * affect the resulting fingerprint.
     *
     * @param path a path template normalised via {@link PathTemplates#normalize(String)}
     * @return {@code path} with every placeholder segment replaced by {@value #PLACEHOLDER_TOKEN}
     */
    private String canonicalize(String path) {
        String[] segments = path.split("/");
        StringBuilder result = new StringBuilder();
        for (String segment : segments) {
            if (segment.isEmpty()) {
                continue;
            }
            result.append('/').append(PathTemplates.isPlaceholder(segment) ? PLACEHOLDER_TOKEN : segment);
        }
        return result.length() == 0 ? "/" : result.toString();
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
