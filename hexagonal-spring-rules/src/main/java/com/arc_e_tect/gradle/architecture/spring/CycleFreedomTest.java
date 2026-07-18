package com.arc_e_tect.gradle.architecture.spring;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Package cycle validation rules for Hexagonal architecture.
 *
 * <p>This rule class enforces cycle freedom inside configured adapter and domain-model
 * package roots. Layer boundary checks can miss cycles inside a single layer, so these
 * rules prevent tightly coupled package graphs that are hard to evolve and test.
 *
 * <p>This class is discovered and included in the rule-pack suite by the Architecture Validator
 * plugin. Each test method defines a separate validation rule.
 *
 * @see RulePackConfiguration
 * @since 1.0.0
 */
class CycleFreedomTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(RulePackConfiguration.basePackage());

    /**
     * Validates that configured adapter package trees are free of package cycles.
     */
    @Test
    void adapterPackagesShouldBeFreeOfCycles() {
        assertConfiguredSlicesAreCycleFree(
                RulePackConfiguration.adapters(),
                "Adapter packages should remain acyclic to keep infrastructure boundaries maintainable");
    }

    /**
     * Validates that configured domain model package trees are free of package cycles.
     */
    @Test
    void domainModelShouldBeFreeOfCycles() {
        assertConfiguredSlicesAreCycleFree(
                RulePackConfiguration.domainModel(),
                "Domain model packages should remain acyclic to preserve a clear core model structure");
    }

    private void assertConfiguredSlicesAreCycleFree(String[] packageRoots, String reason) {
        if (packageRoots.length == 0) {
            return;
        }

        Arrays.stream(packageRoots)
                .map(CycleFreedomTest::toSlicePattern)
                .forEach(slicePattern -> slices()
                        .matching(slicePattern)
                        .should().beFreeOfCycles()
                        .because(reason)
                        .allowEmptyShould(true)
                        .check(classes));
    }

    private static String toSlicePattern(String packageRoot) {
        String trimmed = packageRoot.trim();
        boolean matchesAnyPrefix = trimmed.startsWith("..");

        String normalized = trimmed;
        while (normalized.startsWith("..")) {
            normalized = normalized.substring(2);
        }

        while (normalized.endsWith("..")) {
            normalized = normalized.substring(0, normalized.length() - 2);
        }

        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (normalized.isEmpty()) {
            return "(**)";
        }

        // ArchUnit slice patterns anchor to the start of the fully-qualified class name unless
        // prefixed with "..", so a leading ".." in the configured package root must be preserved,
        // otherwise nested packages (the normal case) never match and the check passes vacuously.
        return (matchesAnyPrefix ? ".." : "") + normalized + ".(**)";
    }
}
