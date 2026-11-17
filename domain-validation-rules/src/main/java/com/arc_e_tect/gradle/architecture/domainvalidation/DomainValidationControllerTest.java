package com.arc_e_tect.gradle.architecture.domainvalidation;

import com.arc_e_tect.gradle.architecture.domainvalidation.ValidationSlices.Slice;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Rules that keep each validation's controller a thin, typed handler of its own contract.
 *
 * <p>Enforces the sections "One handler per validation", "Controller and use case", "Can a
 * single handler serve all validations?", "Package layout" and "Keep the API types and the
 * domain out of the adapter's reach" of the Domain Validation API pattern.
 *
 * <p>A controller is a class in a validation slice that is a Spring {@code @Controller},
 * which includes {@code @RestController}. Its slice's generated code is expected in the
 * slice's {@code adapter.inbound.web.api} package.
 *
 * @see DomainValidationRulePackConfiguration
 * @since 0.1.0
 */
class DomainValidationControllerTest {

    static final String CONTROLLER = "org.springframework.stereotype.Controller";
    private static final String REQUEST_MAPPING = "org.springframework.web.bind.annotation.RequestMapping";

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(DomainValidationRulePackConfiguration.basePackage());

    private final ValidationSlices slices = ValidationSlices.fromConfiguration();

    /**
     * Validates that a controller depends only on its own validation's inbound ports, with
     * their commands and results, on its own inbound web adapter, on the asynchronous
     * execution component, on the JDK and on Spring, so that it cannot accumulate logic.
     */
    @Test
    void controllersDependOnlyOnUseCasesTheirAdapterAndExecution() {
        assumeEnabled("DomainValidationControllerTest.controllersDependOnlyOnUseCasesTheirAdapterAndExecution");
        DescribedPredicate<String> execution = DomainValidationRulePackConfiguration.executionPackages();

        classes()
                .that(areControllers())
                .should(new ArchCondition<>("depend only on their use cases, their own inbound web adapter, the execution component, the JDK and Spring") {
                    @Override
                    public void check(JavaClass controller, ConditionEvents events) {
                        Slice own = slices.sliceOf(controller).orElseThrow();
                        ValidationSlices.dependencyTargets(controller)
                                .filter(target -> !target.getPackageName().startsWith("java.")
                                        && !target.getPackageName().startsWith("org.springframework.")
                                        && !execution.test(target.getPackageName())
                                        && !own.containsInInboundWebAdapter(target)
                                        && !(own.contains(target) && slices.isInboundPort(target)))
                                .map(JavaClass::getName)
                                .distinct()
                                .forEach(target -> events.add(SimpleConditionEvent.violated(controller,
                                        controller.getName() + " depends on " + target)));
                    }
                })
                .because("a controller only maps the typed request to its use case's command, and the use case's result to the typed response")
                .allowEmptyShould(true)
                .check(classes);
    }

    /**
     * Validates that every controller implements an interface generated from the contract
     * into its slice, so that the compiler guarantees a typed handler for every operation.
     */
    @Test
    void controllersImplementAGeneratedApi() {
        assumeEnabled("DomainValidationControllerTest.controllersImplementAGeneratedApi");

        classes()
                .that(areControllers())
                .should(new ArchCondition<>("implement an interface generated into their slice") {
                    @Override
                    public void check(JavaClass controller, ConditionEvents events) {
                        Slice own = slices.sliceOf(controller).orElseThrow();
                        boolean implementsGeneratedApi = controller.getAllRawInterfaces().stream()
                                .anyMatch(own::containsInGeneratedApi);
                        if (!implementsGeneratedApi) {
                            events.add(SimpleConditionEvent.violated(controller,
                                    controller.getName() + " implements no interface of " + own.packageName() + ".adapter.inbound.web.api"));
                        }
                    }
                })
                .because("each validation's operations are generated from its contract, and implemented by one typed handler")
                .allowEmptyShould(true)
                .check(classes);
    }

    /**
     * Validates that a controller declares no request mapping of its own, on the class or on
     * a method: its mappings come from the generated interface, so that no generic handler,
     * such as {@code POST /validations/{name}}, and no path outside the contract can appear.
     */
    @Test
    void controllersDeclareNoRequestMappingsOfTheirOwn() {
        assumeEnabled("DomainValidationControllerTest.controllersDeclareNoRequestMappingsOfTheirOwn");

        classes()
                .that(areControllers())
                .should(new ArchCondition<>("declare no request mappings of their own") {
                    @Override
                    public void check(JavaClass controller, ConditionEvents events) {
                        if (isAnnotatedOrMetaAnnotated(controller.isAnnotatedWith(REQUEST_MAPPING), controller.isMetaAnnotatedWith(REQUEST_MAPPING))) {
                            events.add(SimpleConditionEvent.violated(controller,
                                    controller.getName() + " declares a request mapping on the class"));
                        }
                        for (JavaMethod method : controller.getMethods()) {
                            if (isAnnotatedOrMetaAnnotated(method.isAnnotatedWith(REQUEST_MAPPING), method.isMetaAnnotatedWith(REQUEST_MAPPING))) {
                                events.add(SimpleConditionEvent.violated(method,
                                        method.getFullName() + " declares a request mapping of its own"));
                            }
                        }
                    }
                })
                .because("the request mappings come from the interface generated from the contract, never from the controller")
                .allowEmptyShould(true)
                .check(classes);
    }

    /**
     * Validates that the types generated into a slice's {@code adapter.inbound.web.api}
     * package are used only by that slice's inbound web adapter.
     */
    @Test
    void generatedApiTypesStayInTheirInboundWebAdapter() {
        assumeEnabled("DomainValidationControllerTest.generatedApiTypesStayInTheirInboundWebAdapter");

        classes()
                .should(new ArchCondition<>("use generated types only from their own slice's inbound web adapter") {
                    @Override
                    public void check(JavaClass origin, ConditionEvents events) {
                        ValidationSlices.dependencyTargets(origin)
                                .filter(target -> slices.sliceOf(target)
                                        .filter(slice -> slice.containsInGeneratedApi(target))
                                        .filter(slice -> !slice.containsInInboundWebAdapter(origin))
                                        .isPresent())
                                .map(JavaClass::getName)
                                .distinct()
                                .forEach(target -> events.add(SimpleConditionEvent.violated(origin,
                                        origin.getName() + " uses " + target + ", which is generated for another inbound web adapter")));
                    }
                })
                .because("the code generated from the contract belongs to the inbound web adapter of its slice")
                .allowEmptyShould(true)
                .check(classes);
    }

    private DescribedPredicate<JavaClass> areControllers() {
        return controllers(slices);
    }

    static DescribedPredicate<JavaClass> controllers(ValidationSlices slices) {
        return new DescribedPredicate<>("are controllers of a validation") {
            @Override
            public boolean test(JavaClass javaClass) {
                return slices.sliceOf(javaClass).isPresent()
                        && isAnnotatedOrMetaAnnotated(javaClass.isAnnotatedWith(CONTROLLER), javaClass.isMetaAnnotatedWith(CONTROLLER));
            }
        };
    }

    private static boolean isAnnotatedOrMetaAnnotated(boolean annotated, boolean metaAnnotated) {
        return annotated || metaAnnotated;
    }

    private static void assumeEnabled(String ruleId) {
        Assumptions.assumeFalse(
                DomainValidationRulePackConfiguration.isRuleDisabled(ruleId),
                "Rule disabled via architectureValidator.rules.disabled");
    }
}
