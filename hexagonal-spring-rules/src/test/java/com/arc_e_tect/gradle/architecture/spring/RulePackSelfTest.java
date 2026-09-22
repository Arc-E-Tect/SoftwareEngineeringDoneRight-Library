package com.arc_e_tect.gradle.architecture.spring;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Self-tests for the Spring Hexagonal rule pack.
 *
 * <p>This test suite executes rule classes against in-repo fixture packages to prove
 * that compliant structures pass while targeted violations fail.
 *
 * @since 0.4.0
 */
class RulePackSelfTest {

    private static final String BASE_PACKAGE_KEY = "architectureValidator.basePackage";
    private static final String IN_PORTS_KEY = "architectureValidator.inPorts";
    private static final String OUT_PORTS_KEY = "architectureValidator.outPorts";
    private static final String DOMAIN_MODEL_KEY = "architectureValidator.domainModel";
    private static final String ADAPTERS_KEY = "architectureValidator.adapters";
    private static final String INBOUND_ADAPTERS_KEY = "architectureValidator.inboundAdapters";
    private static final String OUTBOUND_ADAPTERS_KEY = "architectureValidator.outboundAdapters";
    private static final String DOMAIN_SERVICES_KEY = "architectureValidator.domainServices";

    private static final String COMPLIANT_BASE = "com.arc_e_tect.fixtures.compliant";
    private static final String SERVICE_IMPLEMENTS_PORT_BASE = "com.arc_e_tect.fixtures.regression.serviceImplementsPort";

    private static final String SPRING_SERVICES_BASE = "com.arc_e_tect.fixtures.violating.spring.services";
    private static final String SPRING_REPOSITORIES_BASE = "com.arc_e_tect.fixtures.violating.spring.repositories";
    private static final String SPRING_COMPONENTS_BASE = "com.arc_e_tect.fixtures.violating.spring.components";

    private static final String DOMAIN_FRAMEWORK_BASE = "com.arc_e_tect.fixtures.violating.domain.frameworkDependency";
    private static final String DOMAIN_SERVICE_STEREOTYPE_BASE = "com.arc_e_tect.fixtures.violating.domain.applicationServiceStereotype";

    private static final String CYCLE_BASE = "com.arc_e_tect.fixtures.violating.cycle";

    private final Map<String, String> originalProperties = new HashMap<>();

    @BeforeEach
    void captureOriginalArchitectureProperties() {
        capture(BASE_PACKAGE_KEY);
        capture(IN_PORTS_KEY);
        capture(OUT_PORTS_KEY);
        capture(DOMAIN_MODEL_KEY);
        capture(ADAPTERS_KEY);
        capture(INBOUND_ADAPTERS_KEY);
        capture(OUTBOUND_ADAPTERS_KEY);
        capture(DOMAIN_SERVICES_KEY);
    }

    @AfterEach
    void restoreOriginalArchitectureProperties() {
        restore(BASE_PACKAGE_KEY);
        restore(IN_PORTS_KEY);
        restore(OUT_PORTS_KEY);
        restore(DOMAIN_MODEL_KEY);
        restore(ADAPTERS_KEY);
        restore(INBOUND_ADAPTERS_KEY);
        restore(OUTBOUND_ADAPTERS_KEY);
        restore(DOMAIN_SERVICES_KEY);
    }

    @Test
    void rulePackShouldPassAllCoreRulesWhenFixturesAreCompliant() {
        configure(
                COMPLIANT_BASE,
                COMPLIANT_BASE + ".application.port.inbound..",
                COMPLIANT_BASE + ".application.port.outbound..",
                COMPLIANT_BASE + ".domain.model..",
                COMPLIANT_BASE + ".adapters.web..," + COMPLIANT_BASE + ".adapters.persistence..",
                COMPLIANT_BASE + ".application.service.."
        );

        SpringHexagonalArchitectureTest springRules = new SpringHexagonalArchitectureTest();
        DomainIsolationTest domainRules = new DomainIsolationTest();
        CycleFreedomTest cycleRules = new CycleFreedomTest();

        assertAll(
                () -> assertDoesNotThrow(springRules::servicesShouldNotAccessRepositoriesDirectly),
                () -> assertDoesNotThrow(springRules::repositoriesShouldOnlyBeAccessedViaOutPorts),
                () -> assertDoesNotThrow(springRules::springComponentsShouldFollowHexagonalLayers),
                () -> assertDoesNotThrow(domainRules::coreApplicationLayerShouldHaveNoFrameworkDependencies),
                () -> assertDoesNotThrow(domainRules::domainServicesShouldNotCarrySpringStereotypes),
                () -> assertDoesNotThrow(cycleRules::adapterPackagesShouldBeFreeOfCycles),
                () -> assertDoesNotThrow(cycleRules::domainModelShouldBeFreeOfCycles)
        );
    }

    @Test
    void springHexagonalArchitectureShouldPassServicesRuleWhenServiceImplementsItsOwnInPort() {
        configure(
                SERVICE_IMPLEMENTS_PORT_BASE,
                SERVICE_IMPLEMENTS_PORT_BASE + ".application.port.inbound..",
                SERVICE_IMPLEMENTS_PORT_BASE + ".application.port.outbound..",
                SERVICE_IMPLEMENTS_PORT_BASE + ".domain.model..",
                SERVICE_IMPLEMENTS_PORT_BASE + ".adapters..",
                SERVICE_IMPLEMENTS_PORT_BASE + ".application.service.."
        );

        SpringHexagonalArchitectureTest rules = new SpringHexagonalArchitectureTest();

        // Regression test: a @Service implementing its own in-port (the standard Hexagonal
        // pattern) must not be flagged as reaching into a repository/adapter merely because
        // interface implementation is itself a dependency ArchUnit can see. Previously this
        // rule used an allow-list that omitted in-ports, so this exact case failed.
        assertDoesNotThrow(rules::servicesShouldNotAccessRepositoriesDirectly);
    }

    @Test
    void adaptersShouldHonorSplitInboundOutboundAdapterPropertiesWhenLegacyAggregateIsEmpty() {
        // Deliberately leave the legacy ADAPTERS_KEY empty and only configure the split
        // inboundAdapters/outboundAdapters properties, mirroring a consumer who follows the
        // Architecture Validator plugin's preferred split layout without also setting the
        // legacy aggregate. Regression test: RulePackConfiguration.adapters() must honor
        // inboundAdapters/outboundAdapters, not only the legacy aggregate
        // architectureValidator.adapters property.
        System.setProperty(ADAPTERS_KEY, "");
        System.setProperty(INBOUND_ADAPTERS_KEY, "com.example.split.adapters.in..");
        System.setProperty(OUTBOUND_ADAPTERS_KEY, "com.example.split.adapters.out..");

        assertArrayEquals(
                new String[] {"com.example.split.adapters.in..", "com.example.split.adapters.out.."},
                RulePackConfiguration.adapters());
    }

    @Test
    void cycleFreedomTestShouldFailAdapterRuleWhenAdapterPackagesFormACycle() {
        configure(
                CYCLE_BASE,
                CYCLE_BASE + ".application.port.inbound..",
                CYCLE_BASE + ".application.port.outbound..",
                CYCLE_BASE + ".domain.model..",
                // Deliberately a floating "..X.." wildcard, not anchored to CYCLE_BASE like the
                // other fixture configuration in this file: this matches how the real Architecture
                // Validator plugin sends its default adapters pattern (e.g. "..adapter..",
                // "..adapters.."). An anchored, fully-qualified value would not exercise the
                // leading-".." handling this test is targeting.
                "..adapters..",
                CYCLE_BASE + ".application.service.."
        );

        CycleFreedomTest rules = new CycleFreedomTest();

        // Regression test for a bug where CycleFreedomTest stripped the leading ".." from
        // configured package roots before building the ArchUnit slice pattern, anchoring it to
        // the start of the fully-qualified class name so nested packages (the normal case, as
        // exercised here by a base package several segments deep) never matched and the check
        // passed vacuously regardless of real cycles.
        assertThrows(AssertionError.class, rules::adapterPackagesShouldBeFreeOfCycles);
    }

    @Test
    void springHexagonalArchitectureShouldFailServicesRuleWhenServiceDependsOnRepository() {
        configure(
                SPRING_SERVICES_BASE,
                SPRING_SERVICES_BASE + ".application.port.inbound..",
                SPRING_SERVICES_BASE + ".application.port.outbound..",
                SPRING_SERVICES_BASE + ".domain.model..",
                SPRING_SERVICES_BASE + ".adapters..",
                SPRING_SERVICES_BASE + ".application.service.."
        );

        SpringHexagonalArchitectureTest rules = new SpringHexagonalArchitectureTest();

        assertThrows(AssertionError.class, rules::servicesShouldNotAccessRepositoriesDirectly);
        assertDoesNotThrow(rules::repositoriesShouldOnlyBeAccessedViaOutPorts);
        assertDoesNotThrow(rules::springComponentsShouldFollowHexagonalLayers);
    }

    @Test
    void springHexagonalArchitectureShouldFailRepositoryAccessRuleWhenRepositoryIsUsedOutsideAllowedLayers() {
        configure(
                SPRING_REPOSITORIES_BASE,
                SPRING_REPOSITORIES_BASE + ".application.port.inbound..",
                SPRING_REPOSITORIES_BASE + ".application.port.outbound..",
                SPRING_REPOSITORIES_BASE + ".domain.model..",
                SPRING_REPOSITORIES_BASE + ".adapters..",
                SPRING_REPOSITORIES_BASE + ".application.service.."
        );

        SpringHexagonalArchitectureTest rules = new SpringHexagonalArchitectureTest();

        assertDoesNotThrow(rules::servicesShouldNotAccessRepositoriesDirectly);
        assertThrows(AssertionError.class, rules::repositoriesShouldOnlyBeAccessedViaOutPorts);
        assertDoesNotThrow(rules::springComponentsShouldFollowHexagonalLayers);
    }

    @Test
    void springHexagonalArchitectureShouldFailComponentLayerRuleWhenComponentIsOutsideHexagonalPackages() {
        configure(
                SPRING_COMPONENTS_BASE,
                SPRING_COMPONENTS_BASE + ".application.port.inbound..",
                SPRING_COMPONENTS_BASE + ".application.port.outbound..",
                SPRING_COMPONENTS_BASE + ".domain.model..",
                SPRING_COMPONENTS_BASE + ".adapters..",
                SPRING_COMPONENTS_BASE + ".application.service.."
        );

        SpringHexagonalArchitectureTest rules = new SpringHexagonalArchitectureTest();

        assertDoesNotThrow(rules::servicesShouldNotAccessRepositoriesDirectly);
        assertDoesNotThrow(rules::repositoriesShouldOnlyBeAccessedViaOutPorts);
        assertThrows(AssertionError.class, rules::springComponentsShouldFollowHexagonalLayers);
    }

    @Test
    void domainIsolationShouldFailFrameworkDependencyRuleWhenDomainUsesSpringTypes() {
        configure(
                DOMAIN_FRAMEWORK_BASE,
                DOMAIN_FRAMEWORK_BASE + ".application.port.inbound..",
                DOMAIN_FRAMEWORK_BASE + ".application.port.outbound..",
                DOMAIN_FRAMEWORK_BASE + ".domain.model..",
                DOMAIN_FRAMEWORK_BASE + ".adapters..",
                DOMAIN_FRAMEWORK_BASE + ".application.service.."
        );

        DomainIsolationTest baselineRules = new DomainIsolationTest();
        assertDoesNotThrow(baselineRules::coreApplicationLayerShouldHaveNoFrameworkDependencies);
        assertDoesNotThrow(baselineRules::domainServicesShouldNotCarrySpringStereotypes);

        configure(
                DOMAIN_FRAMEWORK_BASE,
                DOMAIN_FRAMEWORK_BASE + ".application.port.inbound..",
                DOMAIN_FRAMEWORK_BASE + ".application.port.outbound..",
                DOMAIN_FRAMEWORK_BASE + ".domain.framework..",
                DOMAIN_FRAMEWORK_BASE + ".adapters..",
                DOMAIN_FRAMEWORK_BASE + ".application.service.."
        );

        DomainIsolationTest violatingRules = new DomainIsolationTest();

        assertThrows(AssertionError.class, violatingRules::coreApplicationLayerShouldHaveNoFrameworkDependencies);
        assertDoesNotThrow(violatingRules::domainServicesShouldNotCarrySpringStereotypes);
    }

    @Test
    void domainIsolationShouldFailServiceStereotypeRuleWhenDomainServiceIsAnnotatedWithService() {
        configure(
                DOMAIN_SERVICE_STEREOTYPE_BASE,
                DOMAIN_SERVICE_STEREOTYPE_BASE + ".application.port.inbound..",
                DOMAIN_SERVICE_STEREOTYPE_BASE + ".application.port.outbound..",
                DOMAIN_SERVICE_STEREOTYPE_BASE + ".domain.model..",
                DOMAIN_SERVICE_STEREOTYPE_BASE + ".adapters..",
                DOMAIN_SERVICE_STEREOTYPE_BASE + ".application.service.."
        );

        DomainIsolationTest rules = new DomainIsolationTest();

        // A domain service annotated with @Service is now caught by both rules: the framework
        // denylist check (an annotation is itself a dependency ArchUnit can see) and the
        // dedicated stereotype check. That's intentional defense-in-depth, not a bug.
        assertThrows(AssertionError.class, rules::coreApplicationLayerShouldHaveNoFrameworkDependencies);
        assertThrows(AssertionError.class, rules::domainServicesShouldNotCarrySpringStereotypes);
    }

    private void capture(String key) {
        originalProperties.put(key, System.getProperty(key));
    }

    private void restore(String key) {
        String value = originalProperties.get(key);
        if (value == null) {
            System.clearProperty(key);
            return;
        }
        System.setProperty(key, value);
    }

    private void configure(
            String basePackage,
            String inPorts,
            String outPorts,
            String domainModel,
            String adapters,
            String domainServices
    ) {
        System.setProperty(BASE_PACKAGE_KEY, basePackage);
        System.setProperty(IN_PORTS_KEY, inPorts);
        System.setProperty(OUT_PORTS_KEY, outPorts);
        System.setProperty(DOMAIN_MODEL_KEY, domainModel);
        System.setProperty(ADAPTERS_KEY, adapters);
        System.setProperty(DOMAIN_SERVICES_KEY, domainServices);
    }
}
