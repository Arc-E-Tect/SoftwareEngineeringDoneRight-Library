package com.arc_e_tect.gradle.architecture.domainvalidation;

import com.arc_e_tect.gradle.architecture.domainvalidation.ValidationSlices.Slice;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Rules that keep every validation a vertical slice of its own.
 *
 * <p>Enforces the sections "Package layout", "Enforcing the structure" and "Adding a
 * validation" of the Domain Validation API pattern, and its rejected alternatives of a
 * central {@code ValidationService}, or one as a façade.
 *
 * @see DomainValidationRulePackConfiguration
 * @since 0.1.0
 */
class DomainValidationSliceTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(DomainValidationRulePackConfiguration.basePackage());

    private final ValidationSlices slices = ValidationSlices.fromConfiguration();

    /**
     * Validates that no validation slice depends on another, so that adding or removing a
     * validation cannot affect another one. The shared packages, such as {@code verdict},
     * belong to no slice and may be used by every slice.
     */
    @Test
    void validationSlicesAreIndependent() {
        assumeEnabled("DomainValidationSliceTest.validationSlicesAreIndependent");

        classes()
                .that(belongToASlice())
                .should(new ArchCondition<>("depend on no other validation slice") {
                    @Override
                    public void check(JavaClass origin, ConditionEvents events) {
                        Slice own = slices.sliceOf(origin).orElseThrow();
                        ValidationSlices.dependencyTargets(origin)
                                .filter(target -> slices.sliceOf(target).filter(slice -> !slice.equals(own)).isPresent())
                                .map(JavaClass::getName)
                                .distinct()
                                .forEach(target -> events.add(SimpleConditionEvent.violated(origin,
                                        origin.getName() + " in validation '" + own.name() + "' depends on " + target + " in another validation")));
                    }
                })
                .because("each validation is a vertical slice, so adding or removing one cannot affect another")
                .allowEmptyShould(true)
                .check(classes);
    }

    /**
     * Validates that the shared packages depend on no slice: a shared package that did would
     * couple every slice that uses it to that one.
     */
    @Test
    void sharedPackagesDependOnNoSlice() {
        assumeEnabled("DomainValidationSliceTest.sharedPackagesDependOnNoSlice");

        classes()
                .that(new DescribedPredicate<>("reside in a shared package") {
                    @Override
                    public boolean test(JavaClass javaClass) {
                        return slices.isShared(javaClass);
                    }
                })
                .should(new ArchCondition<>("depend on no validation slice") {
                    @Override
                    public void check(JavaClass origin, ConditionEvents events) {
                        ValidationSlices.dependencyTargets(origin)
                                .filter(target -> slices.sliceOf(target).isPresent())
                                .map(JavaClass::getName)
                                .distinct()
                                .forEach(target -> events.add(SimpleConditionEvent.violated(origin,
                                        origin.getName() + " is shared by every validation, and depends on " + target + " of one validation")));
                    }
                })
                .because("a shared package that depends on one validation couples every validation to it")
                .allowEmptyShould(true)
                .check(classes);
    }

    /**
     * Validates that no class depends on the inbound ports of more than one validation, so
     * that no central service, façade or registry dispatches to several validations.
     */
    @Test
    void noClassDependsOnMoreThanOneValidation() {
        assumeEnabled("DomainValidationSliceTest.noClassDependsOnMoreThanOneValidation");

        classes()
                .should(new ArchCondition<>("depend on the inbound ports of one validation at most") {
                    @Override
                    public void check(JavaClass origin, ConditionEvents events) {
                        Set<String> used = new TreeSet<>();
                        ValidationSlices.dependencyTargets(origin)
                                .filter(slices::isInboundPort)
                                .map(slices::sliceOf)
                                .flatMap(Optional::stream)
                                .map(Slice::name)
                                .forEach(used::add);
                        if (used.size() > 1) {
                            events.add(SimpleConditionEvent.violated(origin,
                                    origin.getName() + " depends on the inbound ports of the validations " + used));
                        }
                    }
                })
                .because("one service that dispatches to several validations is the central ValidationService the pattern rejects")
                .allowEmptyShould(true)
                .check(classes);
    }

    private DescribedPredicate<JavaClass> belongToASlice() {
        return new DescribedPredicate<>("belong to a validation slice") {
            @Override
            public boolean test(JavaClass javaClass) {
                return slices.sliceOf(javaClass).isPresent();
            }
        };
    }

    private static void assumeEnabled(String ruleId) {
        Assumptions.assumeFalse(
                DomainValidationRulePackConfiguration.isRuleDisabled(ruleId),
                "Rule disabled via architectureValidator.rules.disabled");
    }
}
