package com.arc_e_tect.gradle.architecture.spring;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Port contract validation rules for Hexagonal architecture.
 * 
 * <p>This rule class enforces that ports (communication boundaries between core and adapters)
 * maintain clean, framework-free contracts. Port signatures must only reference domain objects,
 * not framework types or external dependencies.
 * 
 * <p>Rules validate:
 * <ul>
 *   <li>In-ports (input contracts) are interfaces</li>
 *   <li>Out-ports (output contracts) are interfaces</li>
 *   <li>Port signatures only depend on Java core and domain model (no framework leakage)</li>
 * </ul>
 * 
 * <p>This class is discovered and included in the rule-pack suite by the Architecture Validator
 * plugin. Each test method defines a separate validation rule.
 * 
 * @see RulePackConfiguration
 * @since 0.4.0
 */
class PortContractTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(RulePackConfiguration.basePackage());

    /**
     * Validates that input ports (in-bound contracts) are interfaces.
     * 
     * <p>In-ports define how external systems (controllers, REST endpoints, event listeners)
     * interact with the application core. By enforcing interface types, we ensure
     * that external access points are properly abstracted and that the core remains
     * independent of the presentation mechanism.
     */
    @Test
    void inputPortsShouldBeInterfaces() {
        classes()
                .that().resideInAnyPackage(RulePackConfiguration.inPorts())
                .should().beInterfaces()
                .because("Input ports must be interfaces")
                .allowEmptyShould(true)
                .check(classes);
    }

    /**
     * Validates that output ports (out-bound contracts) are interfaces.
     * 
     * <p>Out-ports define how the application core requests external services
     * (repositories, external APIs, message queues). By enforcing interface types,
     * we ensure that the core depends on contracts, not implementations, allowing
     * for flexible adapter implementations.
     */
    @Test
    void outputPortsShouldBeInterfaces() {
        classes()
                .that().resideInAnyPackage(RulePackConfiguration.outPorts())
                .should().beInterfaces()
                .because("Output ports must be interfaces")
                .allowEmptyShould(true)
                .check(classes);
    }

    /**
     * Validates that port signatures only reference core domain and standard Java types.
     * 
     * <p>Port methods must not expose framework types (Spring, Jakarta EE, Hibernate,
     * Jackson) in their signatures. This ensures that ports remain framework-agnostic
     * and that implementation details (persistence, serialization, validation) do not
     * leak into the application boundary.
     */
    @Test
    void portsShouldOnlyDependOnJavaCoreAndDomainModel() {
        classes()
                .that().resideInAnyPackage(RulePackConfiguration.inPorts())
                .or().resideInAnyPackage(RulePackConfiguration.outPorts())
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(RulePackConfiguration.merge(
                        RulePackConfiguration.domainModel(),
                        "java.util..",
                        "java.lang.."))
                .because("Port signatures must not leak framework types into the core application")
                .allowEmptyShould(true)
                .check(classes);
    }
}