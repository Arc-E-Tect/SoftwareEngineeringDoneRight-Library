package com.arc_e_tect.gradle.architecture.domainvalidation;

import com.arc_e_tect.gradle.architecture.domainvalidation.ValidationSlices.Slice;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Rules for the inbound port of each validation: a use case that takes a command and returns
 * a result, as the sealed set of every way it can end.
 *
 * <p>Enforces the sections "Controller and use case" and "Keep the API types and the domain
 * out of the adapter's reach" of the Domain Validation API pattern.
 *
 * <p>Commands are expected in a {@code command} package and results in a {@code result}
 * package below a validation's inbound port package.
 *
 * @see DomainValidationRulePackConfiguration
 * @since 0.1.0
 */
class DomainValidationInboundPortTest {

    private static final String COMMAND = "command";
    private static final String RESULT = "result";

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(DomainValidationRulePackConfiguration.basePackage());

    private final ValidationSlices slices = ValidationSlices.fromConfiguration();

    /**
     * Validates that every method of a validation's use case takes only commands and returns
     * a result, both of its own validation, so that the port never exchanges generated API
     * types or the domain model.
     */
    @Test
    void useCasesTakeACommandAndReturnAResult() {
        assumeEnabled("DomainValidationInboundPortTest.useCasesTakeACommandAndReturnAResult");

        classes()
                .that(new DescribedPredicate<>("are use cases of a validation") {
                    @Override
                    public boolean test(JavaClass javaClass) {
                        return javaClass.isInterface()
                                && portData(javaClass, null)
                                && !ValidationSlices.hasSegment(javaClass.getPackageName(), COMMAND)
                                && !ValidationSlices.hasSegment(javaClass.getPackageName(), RESULT);
                    }
                })
                .should(new ArchCondition<>("take only commands and return a result") {
                    @Override
                    public void check(JavaClass useCase, ConditionEvents events) {
                        for (JavaMethod method : useCase.getMethods()) {
                            if (method.getModifiers().contains(JavaModifier.STATIC) || method.getModifiers().contains(JavaModifier.SYNTHETIC)) {
                                continue;
                            }
                            for (JavaClass parameter : method.getRawParameterTypes()) {
                                if (!isOwn(useCase, parameter, COMMAND)) {
                                    events.add(SimpleConditionEvent.violated(method,
                                            method.getFullName() + " takes " + parameter.getName() + ", which is not a command of its validation"));
                                }
                            }
                            JavaClass returned = method.getRawReturnType();
                            if (!isOwn(useCase, returned, RESULT)) {
                                events.add(SimpleConditionEvent.violated(method,
                                        method.getFullName() + " returns " + returned.getName() + ", which is not a result of its validation"));
                            }
                        }
                    }
                })
                .because("the inbound port owns the types it exchanges: a command in, a result out")
                .allowEmptyShould(true)
                .check(classes);
    }

    /**
     * Validates that every top-level result type is a sealed interface whose permitted
     * subtypes are records, so that the result names every way the use case can end, and the
     * inbound adapter's {@code switch} over it is exhaustive.
     */
    @Test
    void useCaseResultsAreSealed() {
        assumeEnabled("DomainValidationInboundPortTest.useCaseResultsAreSealed");

        classes()
                .that(new DescribedPredicate<>("are results of a validation's use case") {
                    @Override
                    public boolean test(JavaClass javaClass) {
                        return javaClass.isTopLevelClass() && portData(javaClass, RESULT);
                    }
                })
                .should(new ArchCondition<>("be sealed interfaces that permit records only") {
                    @Override
                    public void check(JavaClass result, ConditionEvents events) {
                        if (!result.isInterface() || !result.isSealed()) {
                            events.add(SimpleConditionEvent.violated(result,
                                    result.getName() + " is not a sealed interface"));
                            return;
                        }
                        Set<JavaClass> permitted = result.getPermittedSubclasses().orElse(Set.of());
                        permitted.stream()
                                .filter(subtype -> !subtype.isRecord())
                                .forEach(subtype -> events.add(SimpleConditionEvent.violated(result,
                                        result.getName() + " permits " + subtype.getName() + ", which is not a record")));
                    }
                })
                .because("the result names every way the use case can end, so the inbound adapter has to handle each one")
                .allowEmptyShould(true)
                .check(classes);
    }

    /**
     * Validates that every field of a command is final, so that the input it carries across
     * the port cannot change after the command has checked it.
     */
    @Test
    void commandsAreImmutable() {
        assumeEnabled("DomainValidationInboundPortTest.commandsAreImmutable");

        classes()
                .that(new DescribedPredicate<>("are commands of a validation's use case") {
                    @Override
                    public boolean test(JavaClass javaClass) {
                        return portData(javaClass, COMMAND);
                    }
                })
                .should(new ArchCondition<>("have final fields only") {
                    @Override
                    public void check(JavaClass command, ConditionEvents events) {
                        for (JavaField field : command.getFields()) {
                            if (!field.getModifiers().contains(JavaModifier.SYNTHETIC)
                                    && !field.getModifiers().contains(JavaModifier.FINAL)) {
                                events.add(SimpleConditionEvent.violated(field,
                                        field.getFullName() + " is not final"));
                            }
                        }
                    }
                })
                .because("a command checks its input once, in its constructor, and carries it unchanged")
                .allowEmptyShould(true)
                .check(classes);
    }

    /** A class in a validation's inbound port package, and, when {@code segment} is given, in its sub-package of that name. */
    private boolean portData(JavaClass javaClass, String segment) {
        return slices.sliceOf(javaClass).isPresent()
                && slices.isInboundPort(javaClass)
                && (segment == null || ValidationSlices.hasSegment(javaClass.getPackageName(), segment));
    }

    private boolean isOwn(JavaClass useCase, JavaClass type, String segment) {
        Slice own = slices.sliceOf(useCase).orElseThrow();
        return own.contains(type) && portData(type, segment);
    }

    private static void assumeEnabled(String ruleId) {
        Assumptions.assumeFalse(
                DomainValidationRulePackConfiguration.isRuleDisabled(ruleId),
                "Rule disabled via architectureValidator.rules.disabled");
    }
}
