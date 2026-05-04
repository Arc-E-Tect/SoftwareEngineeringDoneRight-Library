package com.arc_e_tect.sedr.example.jacoco.marker;

import com.arc_e_tect.sedr.utils.jacoco.marker.AbstractCoverageExclusionConventionsTest;

/**
 * Extends {@link AbstractCoverageExclusionConventionsTest} to apply the
 * coverage-exclusion convention check to this example project's packages.
 *
 * <p>Running {@code ./gradlew test} will:
 * <ul>
 *   <li>PASS for {@code CompliantService} (justification is present)</li>
 *   <li>FAIL for {@code NonCompliantService} (no justification)</li>
 * </ul>
 */
class CoverageExclusionConventionsTest extends AbstractCoverageExclusionConventionsTest {

    @Override
    protected String getBasePackage() {
        return "com.arc_e_tect.sedr.example.jacoco.marker";
    }
}
