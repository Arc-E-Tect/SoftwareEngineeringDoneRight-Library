package com.arc_e_tect.gradle.architecture.spring;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Dependency direction validation rules for Hexagonal architecture.
 * 
 * <p>This rule class enforces correct dependency flow in the Hexagonal pattern.
 * Dependencies must only flow inward (from outer adapters toward inner ports and core),
 * never outward (core depending on adapters or specific implementations).
 * 
 * <p>Rules validate:
 * <ul>
 *   <li>Core application does not depend on adapters or adapter implementations</li>
 *   <li>Adapters do not depend on concrete service implementations (only ports)</li>
 *   <li>Only configuration layer may wire concrete services directly</li>
 * </ul>
 * 
 * <p>This class is discovered and included in the rule-pack suite by the Architecture Validator
 * plugin. Each test method defines a separate validation rule.
 * 
 * @see RulePackConfiguration
 * @since 0.4.0
 */
class DependencyDirectionTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(RulePackConfiguration.basePackage());

    /**
     * Validates that the core application layer does not depend on adapters.
     * 
     * <p>Dependencies must flow inward toward the core. The core (domain model and
     * application services) must never depend on adapter implementations or
     * adapter-specific packages. This maintains the Hexagonal architecture boundary
     * and allows adapters to be swapped without affecting the core.
     */
    @Test
    void coreApplicationLayerShouldNotDependOnAdapters() {
        noClasses()
                .that().resideOutsideOfPackage("..adapter..")
                .and().resideOutsideOfPackage("..adapters..")
                .should().dependOnClassesThat().resideInAnyPackage(RulePackConfiguration.adapters())
                .because("Adapters are outer-layer details; the application core must stay independent")
                .check(classes);
    }

    /**
     * Validates that adapters do not depend on concrete service implementations.
     * 
     * <p>Adapters must communicate with the application through ports (interfaces),
     * not by directly referencing concrete service classes. This ensures adapters
     * remain loosely coupled and can be replaced or evolved independently.
     */
    @Test
    void adaptersShouldNotDependOnServiceImplementations() {
        noClasses()
                .that().resideInAnyPackage(RulePackConfiguration.adapters())
                .should().dependOnClassesThat().resideInAnyPackage(RulePackConfiguration.applicationServices())
                .because("Adapters must communicate through ports, not concrete services")
                .allowEmptyShould(true)
                .check(classes);
    }

    /**
     * Validates that only configuration code references concrete service implementations.
     * 
     * <p>Wiring concrete services should be centralized in a configuration layer
     * (typically {@code ..configuration..} or {@code ..application.domain.service..}),
     * never scattered throughout business logic or adapters. This maintains clear
     * boundaries and makes dependencies explicit.
     */
    @Test
    void onlyConfigurationMayDependOnServiceImplementations() {
        noClasses()
                .that().resideOutsideOfPackage("..configuration..")
                .and().resideOutsideOfPackage("..application.domain.service..")
                .should().dependOnClassesThat().resideInAnyPackage(RulePackConfiguration.applicationServices())
                .because("Concrete service implementations should be referenced only by configuration or by their own package")
                .allowEmptyShould(true)
                .check(classes);
    }
}