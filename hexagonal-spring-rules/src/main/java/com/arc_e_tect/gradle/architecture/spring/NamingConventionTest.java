package com.arc_e_tect.gradle.architecture.spring;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Optional naming convention validation rules for Spring Hexagonal architecture.
 *
 * <p>This rule class enforces naming suffix conventions that are genuinely tied to Spring
 * stereotypes, keeping architectural roles obvious in code review and IDE navigation.
 *
 * <p>Rules validate:
 * <ul>
 *   <li>{@code @Repository}/{@code @RestController}/{@code @Controller} adapters use
 *       role-aligned suffixes</li>
 * </ul>
 *
 * <p>Framework-agnostic port/domain-service naming conventions (bidirectional suffix
 * checks) live in the Architecture Validator plugin's built-in Hexagonal rule pack, since
 * they apply equally to non-Spring projects and don't reference the Spring API. There is
 * deliberately no built-in or Spring-pack rule enforcing a single outbound-port suffix:
 * real outbound ports are named for their technical role (Repository, Gateway, Publisher,
 * Generator, ...), and no single suffix fits all of them.
 *
 * <p>This class is discovered and included in the rule-pack suite by the Architecture Validator
 * plugin. Each test method defines a separate validation rule.
 *
 * @see RulePackConfiguration
 * @since 0.4.0
 */
class NamingConventionTest {

    private static final String OPT_IN_MESSAGE = "Naming convention rules are opt-in; set architectureValidator.namingConventions.enabled=true to activate";

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(RulePackConfiguration.basePackage());

    /**
     * Validates that adapter stereotypes use role-aligned suffixes.
     *
     * <p>Repository stereotypes should read as repository/adapter implementations,
     * and web controllers should read as controller entry points.
     */
    @Test
    void adaptersShouldHaveConsistentSuffix() {
        Assumptions.assumeTrue(RulePackConfiguration.namingConventionsEnabled(), OPT_IN_MESSAGE);

        classes()
                .that().resideInAnyPackage(RulePackConfiguration.adapters())
                .and().areAnnotatedWith("org.springframework.stereotype.Repository")
                .should().haveSimpleNameEndingWith("Repository")
                .orShould().haveSimpleNameEndingWith("Adapter")
                .because("Repository adapters should use repository-oriented suffixes to signal their role")
                .allowEmptyShould(true)
                .check(classes);

        classes()
                .that().resideInAnyPackage(RulePackConfiguration.adapters())
                .and().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .should().haveSimpleNameEndingWith("Controller")
                .because("Web adapters should use Controller suffixes to make request-entry components obvious")
                .allowEmptyShould(true)
                .check(classes);

        classes()
                .that().resideInAnyPackage(RulePackConfiguration.adapters())
                .and().areAnnotatedWith("org.springframework.stereotype.Controller")
                .should().haveSimpleNameEndingWith("Controller")
                .because("Web adapters should use Controller suffixes to make request-entry components obvious")
                .allowEmptyShould(true)
                .check(classes);
    }
}
