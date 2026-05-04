package com.arc_e_tect.sedr.example.jacoco.marker;

import com.arc_e_tect.sedr.utils.jacoco.marker.ExcludeFromJacocoGeneratedCodeCoverage;

/**
 * Compliant example — every use of
 * {@link ExcludeFromJacocoGeneratedCodeCoverage} supplies a non-blank
 * {@code justification}, so the ArchUnit convention check will pass for this class.
 */
public class CompliantService {

    @ExcludeFromJacocoGeneratedCodeCoverage(
            justification = "Stub entry point executed only by the container, not unit-testable")
    public void start() {
        // framework entry point — excluded from coverage by design
    }
}
