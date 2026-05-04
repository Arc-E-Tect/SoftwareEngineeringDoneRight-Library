package com.arc_e_tect.sedr.utils.jacoco.marker;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Reusable ArchUnit test that enforces the convention that every use of
 * {@link ExcludeFromJacocoGeneratedCodeCoverage} must supply a non-blank
 * {@code justification} value.
 *
 * <p>Extend this class and implement {@link #getBasePackage()} to scope the
 * rule to your project's root package:
 *
 * <pre>
 *   class MyCoverageExclusionConventionsTest extends AbstractCoverageExclusionConventionsTest {
 *       {@literal @}Override
 *       protected String getBasePackage() {
 *           return "com.example.myapp";
 *       }
 *   }
 * </pre>
 *
 * <p>Add the test-fixtures artifact to your build:
 * <pre>
 *   // Gradle (Groovy DSL)
 *   testImplementation(testFixtures("com.arc-e-tect.sedr.utils:sedr-library:VERSION"))
 * </pre>
 *
 * <p>The single test {@code coverageExclusionAnnotationsMustHaveJustification()}
 * fails if any class, constructor, or method in the scanned packages carries
 * {@code @ExcludeFromJacocoGeneratedCodeCoverage} without a non-blank
 * {@code justification}.
 */
public abstract class AbstractCoverageExclusionConventionsTest {

    /**
     * Returns the root package to scan for classes.
     * All sub-packages are included automatically.
     *
     * @return the base package name, e.g. {@code "com.example.myapp"}
     */
    protected abstract String getBasePackage();

    /**
     * Verifies that every {@code @ExcludeFromJacocoGeneratedCodeCoverage}
     * annotation present in production code carries a non-blank
     * {@code justification} value.
     *
     * <p>An empty or missing justification makes it impossible to distinguish
     * deliberate exclusions from accidental ones during code review.
     */
    @Test
    void coverageExclusionAnnotationsMustHaveJustification() {
        String basePackage = getBasePackage();

        JavaClasses importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(basePackage);

        classes()
                .that().resideInAPackage(basePackage + "..")
                .should(haveNonEmptyJustificationOnAllCoverageExclusionAnnotations())
                .because("@ExcludeFromJacocoGeneratedCodeCoverage without a justification makes it " +
                         "impossible to distinguish deliberate exclusions from accidental ones — " +
                         "always document the reason")
                .check(importedClasses);
    }

    // -------------------------------------------------------------------------
    // Custom condition
    // -------------------------------------------------------------------------

    private static ArchCondition<JavaClass> haveNonEmptyJustificationOnAllCoverageExclusionAnnotations() {
        return new ArchCondition<>("have a non-empty justification on every @ExcludeFromJacocoGeneratedCodeCoverage annotation") {

            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                checkClassAnnotation(clazz, events);
                checkConstructorAnnotations(clazz, events);
                checkMethodAnnotations(clazz, events);
            }

            private void checkClassAnnotation(JavaClass clazz, ConditionEvents events) {
                if (!clazz.isAnnotatedWith(ExcludeFromJacocoGeneratedCodeCoverage.class)) {
                    return;
                }
                ExcludeFromJacocoGeneratedCodeCoverage ann =
                        clazz.getAnnotationOfType(ExcludeFromJacocoGeneratedCodeCoverage.class);
                if (ann.justification().trim().isEmpty()) {
                    events.add(SimpleConditionEvent.violated(clazz,
                            "Class " + clazz.getName() +
                            " uses @ExcludeFromJacocoGeneratedCodeCoverage without a justification"));
                }
            }

            private void checkConstructorAnnotations(JavaClass clazz, ConditionEvents events) {
                for (JavaConstructor ctor : clazz.getConstructors()) {
                    if (!ctor.isAnnotatedWith(ExcludeFromJacocoGeneratedCodeCoverage.class)) {
                        continue;
                    }
                    ExcludeFromJacocoGeneratedCodeCoverage ann =
                            ctor.getAnnotationOfType(ExcludeFromJacocoGeneratedCodeCoverage.class);
                    if (ann.justification().trim().isEmpty()) {
                        events.add(SimpleConditionEvent.violated(clazz,
                                "Constructor " + ctor.getFullName() +
                                " uses @ExcludeFromJacocoGeneratedCodeCoverage without a justification"));
                    }
                }
            }

            private void checkMethodAnnotations(JavaClass clazz, ConditionEvents events) {
                for (JavaMethod method : clazz.getMethods()) {
                    if (!method.isAnnotatedWith(ExcludeFromJacocoGeneratedCodeCoverage.class)) {
                        continue;
                    }
                    ExcludeFromJacocoGeneratedCodeCoverage ann =
                            method.getAnnotationOfType(ExcludeFromJacocoGeneratedCodeCoverage.class);
                    if (ann.justification().trim().isEmpty()) {
                        events.add(SimpleConditionEvent.violated(clazz,
                                "Method " + method.getFullName() +
                                " uses @ExcludeFromJacocoGeneratedCodeCoverage without a justification"));
                    }
                }
            }
        };
    }
}
