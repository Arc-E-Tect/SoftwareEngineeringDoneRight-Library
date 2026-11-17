package com.arc_e_tect.gradle.architecture.domainvalidation;

import com.arc_e_tect.gradle.architecture.domainvalidation.ValidationSlices.Slice;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Optional naming convention of the Domain Validation API pattern: a controller is named
 * after the generated interface it implements, as {@code NameIbanMatchController} implements
 * {@code NameIbanMatchApi}.
 *
 * <p>Enforces the section "Package layout" of the Domain Validation API pattern.
 * Like the naming rules of the Spring rule pack, it runs only when
 * {@code architectureValidator.namingConventions.enabled} is {@code true}.
 *
 * @see DomainValidationRulePackConfiguration
 * @since 0.1.0
 */
class DomainValidationNamingConventionTest {

    private static final String OPT_IN_MESSAGE = "Naming convention rules are opt-in; set architectureValidator.namingConventions.enabled=true to activate";

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(DomainValidationRulePackConfiguration.basePackage());

    private final ValidationSlices slices = ValidationSlices.fromConfiguration();

    /**
     * Validates that a controller implementing {@code XyzApi} is named {@code XyzController}.
     */
    @Test
    void controllersAreNamedAfterTheirApi() {
        Assumptions.assumeTrue(DomainValidationRulePackConfiguration.namingConventionsEnabled(), OPT_IN_MESSAGE);
        Assumptions.assumeFalse(
                DomainValidationRulePackConfiguration.isRuleDisabled("DomainValidationNamingConventionTest.controllersAreNamedAfterTheirApi"),
                "Rule disabled via architectureValidator.rules.disabled");

        classes()
                .that(DomainValidationControllerTest.controllers(slices))
                .should(new ArchCondition<>("be named after the generated interface they implement") {
                    @Override
                    public void check(JavaClass controller, ConditionEvents events) {
                        Slice own = slices.sliceOf(controller).orElseThrow();
                        boolean named = controller.getAllRawInterfaces().stream()
                                .filter(own::containsInGeneratedApi)
                                .map(JavaClass::getSimpleName)
                                .filter(name -> name.endsWith("Api"))
                                .map(name -> name.substring(0, name.length() - "Api".length()) + "Controller")
                                .anyMatch(controller.getSimpleName()::equals);
                        if (!named) {
                            events.add(SimpleConditionEvent.violated(controller,
                                    controller.getName() + " is not named after a generated interface it implements, as XyzController implements XyzApi"));
                        }
                    }
                })
                .because("a controller's name says which validation's operations it handles")
                .allowEmptyShould(true)
                .check(classes);
    }
}
