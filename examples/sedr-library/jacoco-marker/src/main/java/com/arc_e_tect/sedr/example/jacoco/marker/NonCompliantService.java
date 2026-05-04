package com.arc_e_tect.sedr.example.jacoco.marker;

import com.arc_e_tect.sedr.utils.jacoco.marker.ExcludeFromJacocoGeneratedCodeCoverage;

/**
 * Non-compliant example — the annotation is used <em>without</em> a
 * {@code justification}, which is the pattern the ArchUnit convention check
 * is designed to reject.
 *
 * <p>Running {@code ./gradlew test} will report a violation for this class.
 */
public class NonCompliantService {

    @ExcludeFromJacocoGeneratedCodeCoverage
    public void stop() {
        // annotation has no justification — the ArchUnit test will fail here
    }
}
