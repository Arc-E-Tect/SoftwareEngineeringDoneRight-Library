package com.arc_e_tect.gradle.architecture.spring;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Domain isolation validation rules for Hexagonal architecture.
 * 
 * <p>This rule class enforces that the domain model and core application logic remain
 * isolated from framework and external dependencies. This ensures the business logic
 * is reusable, testable, and independent of infrastructure choices.
 * 
 * <p>Rules validate:
 * <ul>
 *   <li>Application core (domain model and domain services) has no Spring, persistence,
 *       or validation framework dependencies</li>
 *   <li>Domain services do not carry Spring stereotypes (remain plain Java)</li>
 * </ul>
 *
 * <p>Framework-agnostic dependency checks (domain model may only depend on domain model
 * or JDK core) live in the Architecture Validator plugin's built-in Hexagonal rule pack —
 * this class only keeps checks that genuinely reference the Spring API.
 * 
 * <p>This class is discovered and included in the rule-pack suite by the Architecture Validator
 * plugin. Each test method defines a separate validation rule.
 * 
 * @see RulePackConfiguration
 * @since 0.4.0
 */
class DomainIsolationTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(RulePackConfiguration.basePackage());

    /**
     * Validates that the core application layer has no Spring or persistence framework dependencies.
     * 
     * <p>The application core (domain model and domain services) must not import
     * Spring, Jakarta EE, Hibernate, Jackson, or other external framework classes.
     * This ensures the business logic remains framework-agnostic and can be evolved
     * independently of infrastructure choices.
     */
    @Test
    void coreApplicationLayerShouldHaveNoFrameworkDependencies() {
        Assumptions.assumeFalse(
                RulePackConfiguration.isRuleDisabled("DomainIsolationTest.coreApplicationLayerShouldHaveNoFrameworkDependencies"),
                "Rule disabled via architectureValidator.rules.disabled"
        );
        noClasses()
                .that().resideInAnyPackage(RulePackConfiguration.merge(
                        RulePackConfiguration.domainServices(),
                        RulePackConfiguration.domainModel()))
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta..",
                        "javax.persistence..",
                        "javax.validation..",
                        "org.hibernate..",
                        "com.fasterxml.jackson..")
                .because("Domain model and domain services must not depend on Spring or persistence frameworks")
                .check(classes);
    }

    /**
     * Validates that domain services remain plain Java classes without Spring stereotypes.
     * 
     * <p>Domain service implementations should not be annotated with {@code @Service}.
     * This keeps the application logic independent of the framework and ensures
     * services are explicitly wired through configuration. The same service
     * class can be used in different frameworks or contexts.
     */
    @Test
    void domainServicesShouldNotCarrySpringStereotypes() {
        Assumptions.assumeFalse(
                RulePackConfiguration.isRuleDisabled("DomainIsolationTest.domainServicesShouldNotCarrySpringStereotypes"),
                "Rule disabled via architectureValidator.rules.disabled"
        );
        noClasses()
                .that().resideInAnyPackage(RulePackConfiguration.domainServices())
                .should().beAnnotatedWith("org.springframework.stereotype.Service")
                .because("Domain service implementations should remain plain Java and be wired explicitly")
                .allowEmptyShould(true)
                .check(classes);
    }
}