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
 * <p>This rule class enforces naming suffix conventions for ports and adapters to keep
 * architectural roles obvious in code review and IDE navigation.
 *
 * <p>Rules validate:
 * <ul>
 *   <li>In-ports end with {@code UseCase} or {@code Port}</li>
 *   <li>Out-ports end with {@code Port}</li>
 *   <li>Repository and web adapter stereotypes use consistent suffixes</li>
 * </ul>
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
     * Validates that in-port types use a consistent role suffix.
     *
     * <p>Using {@code UseCase} or {@code Port} suffixes makes inbound application
     * contracts instantly recognizable and reinforces Hexagonal boundaries.
     */
    @Test
    void inputPortsShouldHaveConsistentSuffix() {
        Assumptions.assumeTrue(RulePackConfiguration.namingConventionsEnabled(), OPT_IN_MESSAGE);
        classes()
                .that().resideInAnyPackage(RulePackConfiguration.inPorts())
                .should().haveNameMatching(".*UseCase$")
                .orShould().haveNameMatching(".*Port$")
                .because("Consistent in-port naming makes application entry-point contracts explicit")
                .allowEmptyShould(true)
                .check(classes);
    }

    /**
     * Validates that out-port types end with {@code Port}.
     *
     * <p>A stable suffix for outbound contracts clarifies that adapter implementations
     * are behind an abstraction owned by the application core.
     */
    @Test
    void outputPortsShouldHaveConsistentSuffix() {
        Assumptions.assumeTrue(RulePackConfiguration.namingConventionsEnabled(), OPT_IN_MESSAGE);
        classes()
                .that().resideInAnyPackage(RulePackConfiguration.outPorts())
                .should().haveSimpleNameEndingWith("Port")
                .because("Consistent out-port naming keeps infrastructure boundaries self-documenting")
                .allowEmptyShould(true)
                .check(classes);
    }

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
