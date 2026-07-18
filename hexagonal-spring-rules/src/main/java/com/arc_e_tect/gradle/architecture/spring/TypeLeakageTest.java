package com.arc_e_tect.gradle.architecture.spring;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/**
 * Type leakage validation rules for Spring Hexagonal architecture.
 *
 * <p>This rule class enforces that persistence and web transport details do not leak
 * into ports or the domain model. Ports and the core model must depend on stable
 * business types, not JPA entities or web DTOs.
 *
 * <p>Rules validate:
 * <ul>
 *   <li>In-ports and out-ports do not expose JPA entities</li>
 *   <li>Domain model does not reference JPA entities</li>
 *   <li>Ports do not expose web DTO-style types</li>
 * </ul>
 *
 * <p>This class is discovered and included in the rule-pack suite by the Architecture Validator
 * plugin. Each test method defines a separate validation rule.
 *
 * @see RulePackConfiguration
 * @since 1.0.0
 */
class TypeLeakageTest {

        private static final String JAKARTA_ENTITY = "jakarta.persistence.Entity";
        private static final String JAVAX_ENTITY = "javax.persistence.Entity";

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(RulePackConfiguration.basePackage());

    /**
     * Validates that ports do not expose JPA entity types.
     *
     * <p>Ports define application contracts and must stay persistence-agnostic.
     * Referencing JPA entities in port contracts leaks adapter concerns into
     * the application boundary and couples core use cases to persistence models.
     */
    @Test
    void portsShouldNotExposeJpaEntities() {
        methods()
                .that().areDeclaredInClassesThat().resideInAnyPackage(RulePackConfiguration.inPorts())
                .or().areDeclaredInClassesThat().resideInAnyPackage(RulePackConfiguration.outPorts())
                .should(notExposeJpaEntityTypesInMethodSignatures())
                .because("Ports must not expose JPA entity types; use domain model contracts instead")
                .allowEmptyShould(true)
                .check(classes);
    }

    /**
     * Validates that domain model types do not reference JPA entities.
     *
     * <p>The domain model must remain persistence-independent so business
     * behavior can evolve independently from ORM mappings and storage concerns.
     */
    @Test
    void domainModelShouldNotReferenceJpaEntities() {
        classes()
                .that().resideInAnyPackage(RulePackConfiguration.domainModel())
                .should(notDependOnJpaEntityTypes())
                .because("Domain model must not depend on JPA entity types")
                .allowEmptyShould(true)
                .check(classes);
    }

    /**
     * Validates that ports do not expose web DTO/request/response transport types.
     *
     * <p>Port contracts should model business interactions, not HTTP transport payloads.
     * Using web DTO packages in ports leaks adapter concerns into the application core.
     */
    @Test
    void portsShouldNotExposeWebDtos() {
                methods()
                                .that().areDeclaredInClassesThat().resideInAnyPackage(RulePackConfiguration.inPorts())
                                .or().areDeclaredInClassesThat().resideInAnyPackage(RulePackConfiguration.outPorts())
                                .should(notExposeWebDtoTypesInMethodSignatures())
                .because("Ports must not expose web transport DTOs; map them at adapter boundaries")
                .allowEmptyShould(true)
                .check(classes);
    }

        private static ArchCondition<JavaMethod> notExposeJpaEntityTypesInMethodSignatures() {
                return new ArchCondition<>("not expose JPA entity types in method signatures") {
                        @Override
                        public void check(JavaMethod method, ConditionEvents events) {
                                verifySignatureTypeIsNotJpaEntity(method, method.getRawReturnType(), "return type", events);
                                method.getRawParameterTypes().forEach(parameterType ->
                                                verifySignatureTypeIsNotJpaEntity(method, parameterType, "parameter type", events));
                        }
                };
        }

        private static ArchCondition<JavaClass> notDependOnJpaEntityTypes() {
                return new ArchCondition<>("not depend on JPA entity types") {
                        @Override
                        public void check(JavaClass javaClass, ConditionEvents events) {
                                for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                                        JavaClass targetClass = dependency.getTargetClass();
                                        if (isJpaEntityType(targetClass)) {
                                                events.add(SimpleConditionEvent.violated(
                                                                javaClass,
                                                                javaClass.getName() + " depends on JPA entity type " + targetClass.getName()));
                                        }
                                }
                        }
                };
        }

        private static ArchCondition<JavaMethod> notExposeWebDtoTypesInMethodSignatures() {
                return new ArchCondition<>("not expose web DTO types in method signatures") {
                        @Override
                        public void check(JavaMethod method, ConditionEvents events) {
                                verifySignatureTypeIsNotWebDto(method, method.getRawReturnType(), "return type", events);
                                method.getRawParameterTypes().forEach(parameterType ->
                                                verifySignatureTypeIsNotWebDto(method, parameterType, "parameter type", events));
                        }
                };
        }

        private static void verifySignatureTypeIsNotJpaEntity(JavaMethod method, JavaClass signatureType, String role, ConditionEvents events) {
                if (isJpaEntityType(signatureType)) {
                        events.add(SimpleConditionEvent.violated(
                                        method,
                                        method.getOwner().getName() + "#" + method.getName() + " exposes JPA entity "
                                                        + signatureType.getName() + " as " + role));
                }
        }

        private static void verifySignatureTypeIsNotWebDto(JavaMethod method, JavaClass signatureType, String role, ConditionEvents events) {
                if (isWebDtoType(signatureType)) {
                        events.add(SimpleConditionEvent.violated(
                                        method,
                                        method.getOwner().getName() + "#" + method.getName() + " exposes web DTO type "
                                                        + signatureType.getName() + " as " + role));
                }
        }

        private static boolean isJpaEntityType(JavaClass javaClass) {
                return javaClass.getAnnotations().stream()
                                .map(annotation -> annotation.getRawType().getName())
                                .anyMatch(annotationName -> JAKARTA_ENTITY.equals(annotationName) || JAVAX_ENTITY.equals(annotationName));
        }

        private static boolean isWebDtoType(JavaClass javaClass) {
                String typeName = javaClass.getName().toLowerCase();
                return typeName.contains(".web.")
                                && (typeName.contains(".dto.")
                                || typeName.contains(".request.")
                                || typeName.contains(".response."));
        }
}
