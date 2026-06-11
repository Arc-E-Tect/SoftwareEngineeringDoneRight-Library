package com.arc_e_tect.gradle.architecture.spring;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Spring Hexagonal architecture validation rules.
 * 
 * <p>This rule class enforces architectural constraints for Spring applications that follow
 * the Hexagonal (ports and adapters) pattern. It validates that Spring-specific components
 * (controllers, services, repositories) respect the boundaries between application layers
 * (in-ports, out-ports, domain model, adapters, application services).
 * 
 * <p>Rules validate:
 * <ul>
 *   <li>Controllers only call in-ports (not direct service or adapter access)</li>
 *   <li>Services do not directly access repositories (must use out-ports)</li>
 *   <li>Repositories are only accessed through out-ports</li>
 *   <li>Spring components reside in appropriate Hexagonal layers</li>
 * </ul>
 * 
 * <p>This class is discovered and included in the rule-pack suite by the Architecture Validator
 * plugin. Each test method defines a separate validation rule.
 * 
 * @see RulePackConfiguration
 * @since 0.4.0
 */
class SpringHexagonalArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(RulePackConfiguration.basePackage());

    private String[] mergeAll(String[] first, String[] second, String[] third, String... fixed) {
        String[] merged = RulePackConfiguration.merge(first, fixed);
        merged = RulePackConfiguration.merge(second, merged);
        return RulePackConfiguration.merge(third, merged);
    }

    private String[] mergeAll(String[] first, String[] second, String... fixed) {
        String[] merged = RulePackConfiguration.merge(first, fixed);
        return RulePackConfiguration.merge(second, merged);
    }

    /**
     * Validates that Spring controllers only depend on in-ports (and core Java/Spring classes).
     * 
     * <p>Controllers must communicate with the application through defined in-ports,
     * never directly accessing services, adapters, or repositories. This maintains
     * the Hexagonal boundary and allows the business logic to remain independent
     * of the presentation layer.
     */
    @Test
    void controllersShouldOnlyCallInPorts() {
        classes()
                .that().areAnnotatedWith("org.springframework.stereotype.Controller")
                .or().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(RulePackConfiguration.merge(
                        RulePackConfiguration.inPorts(),
                        "java..",
                        "org.springframework.."))
                .because("Spring controllers should only call in-ports to maintain Hexagonal Architecture")
                .allowEmptyShould(true)
                .check(classes);
    }

    /**
     * Validates that Spring services do not directly access repositories or adapters.
     * 
     * <p>Services must communicate with external systems through out-ports only.
     * Direct repository access couples the business logic to persistence details
     * and prevents flexibility in choosing adapter implementations.
     */
    @Test
    void servicesShouldNotAccessRepositoriesDirectly() {
        classes()
                .that().areAnnotatedWith("org.springframework.stereotype.Service")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(mergeAll(
                        RulePackConfiguration.outPorts(),
                        RulePackConfiguration.applicationServices(),
                        RulePackConfiguration.domainModel(),
                        "java..",
                        "org.springframework.."))
                .because("Spring services should not access repositories directly; use out-ports instead")
                .allowEmptyShould(true)
                .check(classes);
    }

    /**
     * Validates that Spring repositories are only accessed through out-ports.
     * 
     * <p>Repositories are adapters that implement out-port interfaces. Direct
     * repository access from other code violates the Hexagonal pattern and
     * creates unwanted coupling to persistence infrastructure.
     */
    @Test
    void repositoriesShouldOnlyBeAccessedViaOutPorts() {
        classes()
                .that().areAnnotatedWith("org.springframework.stereotype.Repository")
                .should().onlyBeAccessed().byClassesThat()
                .resideInAnyPackage(mergeAll(
                        RulePackConfiguration.outPorts(),
                        RulePackConfiguration.adapters(),
                        "..configuration.."))
                .because("Spring repositories should only be accessed via out-ports")
                .allowEmptyShould(true)
                .check(classes);
    }

    /**
     * Validates that Spring components reside in appropriate Hexagonal layers.
     * 
     * <p>Components must be located in application services, domain model,
     * or adapter packages. Components outside these layers indicate misplaced
     * business logic or infrastructure concerns.
     */
    @Test
    void springComponentsShouldFollowHexagonalLayers() {
        classes()
                .that().areAnnotatedWith("org.springframework.stereotype.Component")
                .or().areAnnotatedWith("org.springframework.stereotype.Service")
                .or().areAnnotatedWith("org.springframework.stereotype.Repository")
                .should().resideInAnyPackage(mergeAll(
                        RulePackConfiguration.applicationServices(),
                        RulePackConfiguration.domainModel(),
                        RulePackConfiguration.adapters()))
                .because("Spring components should follow Hexagonal layers")
                .allowEmptyShould(true)
                .check(classes);
    }
}