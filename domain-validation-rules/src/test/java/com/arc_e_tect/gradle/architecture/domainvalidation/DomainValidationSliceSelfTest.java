package com.arc_e_tect.gradle.architecture.domainvalidation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("The slice rules")
class DomainValidationSliceSelfTest extends RuleSelfTestSupport {

    @Test
    @DisplayName("let slices depend on the shared verdict model, and not on each other")
    void independentSlicesPass() {
        configure("compliant");

        assertDoesNotThrow(new DomainValidationSliceTest()::validationSlicesAreIndependent);
    }

    @Test
    @DisplayName("fail when one validation slice depends on another")
    void dependentSlicesFail() {
        configure("violating.slicedependency");

        assertThrows(AssertionError.class, new DomainValidationSliceTest()::validationSlicesAreIndependent);
    }

    @Test
    @DisplayName("let the shared packages depend on no slice")
    void sharedPackagesWithoutSliceDependenciesPass() {
        configure("compliant");

        assertDoesNotThrow(new DomainValidationSliceTest()::sharedPackagesDependOnNoSlice);
    }

    @Test
    @DisplayName("fail when a shared package depends on a slice")
    void sharedPackageDependingOnASliceFails() {
        configure("violating.shareddependsonslice");

        assertThrows(AssertionError.class, new DomainValidationSliceTest()::sharedPackagesDependOnNoSlice);
    }

    @Test
    @DisplayName("let each class use the inbound ports of one validation at most")
    void classesUsingOneValidationPass() {
        configure("compliant");

        assertDoesNotThrow(new DomainValidationSliceTest()::noClassDependsOnMoreThanOneValidation);
    }

    @Test
    @DisplayName("fail for a central dispatcher that uses the inbound ports of two validations")
    void centralDispatcherFails() {
        configure("violating.centraldispatcher");

        assertThrows(AssertionError.class, new DomainValidationSliceTest()::noClassDependsOnMoreThanOneValidation);
    }

    @Test
    @DisplayName("skip a rule listed in architectureValidator.rules.disabled")
    void disabledRuleIsSkipped() {
        configure("violating.slicedependency");
        disableRule("DomainValidationSliceTest.validationSlicesAreIndependent");

        assertThrows(TestAbortedException.class, new DomainValidationSliceTest()::validationSlicesAreIndependent);
    }
}
