package com.arc_e_tect.gradle.architecture.spring;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
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
 *   <li>Domain model only depends on Java core classes and other domain classes</li>
 *   <li>Application core has no Spring, persistence, or validation framework dependencies</li>
 *   <li>Application services do not carry Spring stereotypes (remain plain Java)</li>
 * </ul>
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
     * Validates that the domain model is framework-free and only depends on core Java classes.
     * 
     * <p>Domain model classes must be portable and reusable without any dependency on
     * Spring, persistence frameworks, or other external libraries. This ensures the
     * business logic can be tested, evolved, and potentially reused in different contexts.
     */
    @Test
    void domainModelShouldOnlyDependOnJavaCoreAndDomainModel() {
        classes()
                .that().resideInAnyPackage(RulePackConfiguration.domainModel())
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(RulePackConfiguration.merge(
                        RulePackConfiguration.domainModel(),
    /**
     * Validates that the core application layer has no Spring or persistence framework dependencies.
     * 
     * <p>The application core (domain model and business services) must not import
     * Spring, Jakarta EE, Hibernate, Jackson, or other external framework classes.
     * This ensures the business logic remains framework-agnostic and can be evolved
     * independently of infrastructure choices.
     */
                        "java.util..",
                        "java.lang..",
                        "java.time.."))
                .because("Domain model classes must stay framework free")
                .check(classes);
    }

    @Test
    void coreApplicationLayerShouldHaveNoFrameworkDependencies() {
        noClasses()
                .that().resideInAnyPackage(RulePackConfiguration.domainModel())
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta..",
                        "javax.persistence..",
                        "javax.validation..",
                        "org.hibernate..",
                        "com.fasterxml.jackson..")
                .because("Domain model must not depend on Spring or persistence frameworks")
                .check(classes);
    }

    /**
     * Validates that application services remain plain Java classes without Spring stereotypes.
     * 
     * <p>Service implementations should not be annotated with {@code @Service}.
     * This keeps the application logic independent of the framework and ensures
     * services are explicitly wired through configuration. The same service
     * class can be used in different frameworks or contexts.
     */
    @Test
    void applicationServicesShouldNotCarrySpringStereotypes() {
        noClasses()
                .that().resideInAnyPackage(RulePackConfiguration.applicationServices())
                .should().beAnnotatedWith("org.springframework.stereotype.Service")
                .because("Service implementations should remain plain Java and be wired explicitly")
                .allowEmptyShould(true)
                .check(classes);
    }
}