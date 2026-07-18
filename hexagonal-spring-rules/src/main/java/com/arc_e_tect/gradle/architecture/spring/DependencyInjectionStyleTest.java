package com.arc_e_tect.gradle.architecture.spring;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * Dependency injection style validation rules for Hexagonal architecture.
 *
 * <p>This rule class enforces constructor-injection-friendly design by forbidding
 * Spring field injection annotations in core application and adapter layers.
 *
 * <p>Rules validate:
 * <ul>
 *   <li>Fields in adapters, application services, and domain model are not annotated with {@code @Autowired}</li>
 * </ul>
 *
 * <p>This class is discovered and included in the rule-pack suite by the Architecture Validator
 * plugin. Each test method defines a separate validation rule.
 *
 * @see RulePackConfiguration
 * @since 0.4.0
 */
class DependencyInjectionStyleTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(RulePackConfiguration.basePackage());

    private String[] targetPackages() {
        return RulePackConfiguration.merge(
                RulePackConfiguration.domainModel(),
                RulePackConfiguration.merge(
                        RulePackConfiguration.adapters(),
                        RulePackConfiguration.applicationServices())
        );
    }

    /**
     * Validates that classes in core and adapter layers do not use field injection.
     *
     * <p>Field injection hides required dependencies and makes classes harder to test in isolation.
     * Constructor injection keeps dependencies explicit and supports framework-agnostic design.
     */
    @Test
    void fieldsShouldNotBeAutowired() {
        noFields()
                .that().areDeclaredInClassesThat().resideInAnyPackage(targetPackages())
                .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
                .because("Constructor injection keeps dependencies explicit and testable without a Spring context")
                .allowEmptyShould(true)
                .check(classes);
    }
}
