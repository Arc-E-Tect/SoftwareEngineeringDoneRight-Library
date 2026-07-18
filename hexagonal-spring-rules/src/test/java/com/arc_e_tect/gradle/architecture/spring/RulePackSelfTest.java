package com.arc_e_tect.gradle.architecture.spring;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
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
    private static final String APPLICATION_SERVICES_KEY = "architectureValidator.applicationServices";

    private static final String COMPLIANT_BASE = "com.arc_e_tect.fixtures.compliant";
    private static final String SERVICE_IMPLEMENTS_PORT_BASE = "com.arc_e_tect.fixtures.regression.serviceImplementsPort";

    private static final String SPRING_CONTROLLERS_BASE = "com.arc_e_tect.fixtures.violating.spring.controllers";
    private static final String SPRING_SERVICES_BASE = "com.arc_e_tect.fixtures.violating.spring.services";
    private static final String SPRING_REPOSITORIES_BASE = "com.arc_e_tect.fixtures.violating.spring.repositories";
    private static final String SPRING_COMPONENTS_BASE = "com.arc_e_tect.fixtures.violating.spring.components";

    private static final String DOMAIN_DEPENDENCY_BASE = "com.arc_e_tect.fixtures.violating.domain.domainDependency";
    private static final String DOMAIN_FRAMEWORK_BASE = "com.arc_e_tect.fixtures.violating.domain.frameworkDependency";
    private static final String DOMAIN_SERVICE_STEREOTYPE_BASE = "com.arc_e_tect.fixtures.violating.domain.applicationServiceStereotype";

    private static final String DEPENDENCY_CORE_BASE = "com.arc_e_tect.fixtures.violating.dependency.coreDependsOnAdapter";
    private static final String DEPENDENCY_ADAPTER_BASE = "com.arc_e_tect.fixtures.violating.dependency.adapterDependsOnService";
    private static final String DEPENDENCY_NON_CONFIG_BASE = "com.arc_e_tect.fixtures.violating.dependency.nonConfigDependsOnService";

    private static final String PORT_INPUT_BASE = "com.arc_e_tect.fixtures.violating.port.inputNotInterface";
    private static final String PORT_OUTPUT_BASE = "com.arc_e_tect.fixtures.violating.port.outputNotInterface";
    private static final String PORT_SIGNATURE_BASE = "com.arc_e_tect.fixtures.violating.port.signatureLeak";

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
        capture(APPLICATION_SERVICES_KEY);
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
        restore(APPLICATION_SERVICES_KEY);
    }

    @Test
    void rulePackShouldPassAllCoreRulesWhenFixturesAreCompliant() {
        configure(
                COMPLIANT_BASE,
                COMPLIANT_BASE + ".application.port.in..",
                COMPLIANT_BASE + ".application.port.out..",
                COMPLIANT_BASE + ".domain.model..",
                COMPLIANT_BASE + ".adapters.web..," + COMPLIANT_BASE + ".adapters.persistence..",
                COMPLIANT_BASE + ".application.service.."
        );

        SpringHexagonalArchitectureTest springRules = new SpringHexagonalArchitectureTest();
        DomainIsolationTest domainRules = new DomainIsolationTest();
        DependencyDirectionTest dependencyRules = new DependencyDirectionTest();
        PortContractTest portRules = new PortContractTest();

        CycleFreedomTest cycleRules = new CycleFreedomTest();

        assertAll(
                () -> assertDoesNotThrow(springRules::controllersShouldOnlyCallInPorts),
                () -> assertDoesNotThrow(springRules::servicesShouldNotAccessRepositoriesDirectly),
                () -> assertDoesNotThrow(springRules::repositoriesShouldOnlyBeAccessedViaOutPorts),
                () -> assertDoesNotThrow(springRules::springComponentsShouldFollowHexagonalLayers),
                () -> assertDoesNotThrow(domainRules::domainModelShouldOnlyDependOnJavaCoreAndDomainModel),
                () -> assertDoesNotThrow(domainRules::coreApplicationLayerShouldHaveNoFrameworkDependencies),
                () -> assertDoesNotThrow(domainRules::applicationServicesShouldNotCarrySpringStereotypes),
                () -> assertDoesNotThrow(dependencyRules::coreApplicationLayerShouldNotDependOnAdapters),
                () -> assertDoesNotThrow(dependencyRules::adaptersShouldNotDependOnServiceImplementations),
                () -> assertDoesNotThrow(dependencyRules::onlyConfigurationMayDependOnServiceImplementations),
                () -> assertDoesNotThrow(portRules::inputPortsShouldBeInterfaces),
                () -> assertDoesNotThrow(portRules::outputPortsShouldBeInterfaces),
                () -> assertDoesNotThrow(portRules::portsShouldOnlyDependOnJavaCoreAndDomainModel),
                () -> assertDoesNotThrow(cycleRules::adapterPackagesShouldBeFreeOfCycles),
                () -> assertDoesNotThrow(cycleRules::domainModelShouldBeFreeOfCycles)
        );
    }

    @Test
    void springHexagonalArchitectureShouldPassServicesRuleWhenServiceImplementsItsOwnInPort() {
        configure(
                SERVICE_IMPLEMENTS_PORT_BASE,
                SERVICE_IMPLEMENTS_PORT_BASE + ".application.port.in..",
                SERVICE_IMPLEMENTS_PORT_BASE + ".application.port.out..",
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
    void cycleFreedomTestShouldFailAdapterRuleWhenAdapterPackagesFormACycle() {
        configure(
                CYCLE_BASE,
                CYCLE_BASE + ".application.port.in..",
                CYCLE_BASE + ".application.port.out..",
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
    void springHexagonalArchitectureShouldFailControllersRuleWhenControllerDependsOnService() {
        configure(
                SPRING_CONTROLLERS_BASE,
                SPRING_CONTROLLERS_BASE + ".application.port.in..",
                SPRING_CONTROLLERS_BASE + ".application.port.out..",
                SPRING_CONTROLLERS_BASE + ".domain.model..",
                SPRING_CONTROLLERS_BASE + ".adapters..",
                SPRING_CONTROLLERS_BASE + ".application.service.."
        );

        SpringHexagonalArchitectureTest rules = new SpringHexagonalArchitectureTest();

        assertThrows(AssertionError.class, rules::controllersShouldOnlyCallInPorts);
        assertDoesNotThrow(rules::servicesShouldNotAccessRepositoriesDirectly);
        assertDoesNotThrow(rules::repositoriesShouldOnlyBeAccessedViaOutPorts);
        assertDoesNotThrow(rules::springComponentsShouldFollowHexagonalLayers);
    }

    @Test
    void springHexagonalArchitectureShouldFailServicesRuleWhenServiceDependsOnRepository() {
        configure(
                SPRING_SERVICES_BASE,
                SPRING_SERVICES_BASE + ".application.port.in..",
                SPRING_SERVICES_BASE + ".application.port.out..",
                SPRING_SERVICES_BASE + ".domain.model..",
                SPRING_SERVICES_BASE + ".adapters..",
                SPRING_SERVICES_BASE + ".application.service.."
        );

        SpringHexagonalArchitectureTest rules = new SpringHexagonalArchitectureTest();

        assertDoesNotThrow(rules::controllersShouldOnlyCallInPorts);
        assertThrows(AssertionError.class, rules::servicesShouldNotAccessRepositoriesDirectly);
        assertDoesNotThrow(rules::repositoriesShouldOnlyBeAccessedViaOutPorts);
        assertDoesNotThrow(rules::springComponentsShouldFollowHexagonalLayers);
    }

    @Test
    void springHexagonalArchitectureShouldFailRepositoryAccessRuleWhenRepositoryIsUsedOutsideAllowedLayers() {
        configure(
                SPRING_REPOSITORIES_BASE,
                SPRING_REPOSITORIES_BASE + ".application.port.in..",
                SPRING_REPOSITORIES_BASE + ".application.port.out..",
                SPRING_REPOSITORIES_BASE + ".domain.model..",
                SPRING_REPOSITORIES_BASE + ".adapters..",
                SPRING_REPOSITORIES_BASE + ".application.service.."
        );

        SpringHexagonalArchitectureTest rules = new SpringHexagonalArchitectureTest();

        assertDoesNotThrow(rules::controllersShouldOnlyCallInPorts);
        assertDoesNotThrow(rules::servicesShouldNotAccessRepositoriesDirectly);
        assertThrows(AssertionError.class, rules::repositoriesShouldOnlyBeAccessedViaOutPorts);
        assertDoesNotThrow(rules::springComponentsShouldFollowHexagonalLayers);
    }

    @Test
    void springHexagonalArchitectureShouldFailComponentLayerRuleWhenComponentIsOutsideHexagonalPackages() {
        configure(
                SPRING_COMPONENTS_BASE,
                SPRING_COMPONENTS_BASE + ".application.port.in..",
                SPRING_COMPONENTS_BASE + ".application.port.out..",
                SPRING_COMPONENTS_BASE + ".domain.model..",
                SPRING_COMPONENTS_BASE + ".adapters..",
                SPRING_COMPONENTS_BASE + ".application.service.."
        );

        SpringHexagonalArchitectureTest rules = new SpringHexagonalArchitectureTest();

        assertDoesNotThrow(rules::controllersShouldOnlyCallInPorts);
        assertDoesNotThrow(rules::servicesShouldNotAccessRepositoriesDirectly);
        assertDoesNotThrow(rules::repositoriesShouldOnlyBeAccessedViaOutPorts);
        assertThrows(AssertionError.class, rules::springComponentsShouldFollowHexagonalLayers);
    }

    @Test
    void domainIsolationShouldFailDomainDependencyRuleWhenDomainDependsOnPortContract() {
        configure(
                DOMAIN_DEPENDENCY_BASE,
                DOMAIN_DEPENDENCY_BASE + ".application.port.in..",
                DOMAIN_DEPENDENCY_BASE + ".application.port.out..",
                DOMAIN_DEPENDENCY_BASE + ".domain.model..",
                DOMAIN_DEPENDENCY_BASE + ".adapters..",
                DOMAIN_DEPENDENCY_BASE + ".application.service.."
        );

        DomainIsolationTest rules = new DomainIsolationTest();

        assertThrows(AssertionError.class, rules::domainModelShouldOnlyDependOnJavaCoreAndDomainModel);
        assertDoesNotThrow(rules::coreApplicationLayerShouldHaveNoFrameworkDependencies);
        assertDoesNotThrow(rules::applicationServicesShouldNotCarrySpringStereotypes);
    }

    @Test
    void domainIsolationShouldFailFrameworkDependencyRuleWhenDomainUsesSpringTypes() {
        configure(
                DOMAIN_FRAMEWORK_BASE,
                DOMAIN_FRAMEWORK_BASE + ".application.port.in..",
                DOMAIN_FRAMEWORK_BASE + ".application.port.out..",
                DOMAIN_FRAMEWORK_BASE + ".domain.model..",
                DOMAIN_FRAMEWORK_BASE + ".adapters..",
                DOMAIN_FRAMEWORK_BASE + ".application.service.."
        );

        DomainIsolationTest baselineRules = new DomainIsolationTest();
        assertDoesNotThrow(baselineRules::domainModelShouldOnlyDependOnJavaCoreAndDomainModel);
        assertDoesNotThrow(baselineRules::applicationServicesShouldNotCarrySpringStereotypes);

        configure(
                DOMAIN_FRAMEWORK_BASE,
                DOMAIN_FRAMEWORK_BASE + ".application.port.in..",
                DOMAIN_FRAMEWORK_BASE + ".application.port.out..",
                DOMAIN_FRAMEWORK_BASE + ".domain.framework..",
                DOMAIN_FRAMEWORK_BASE + ".adapters..",
                DOMAIN_FRAMEWORK_BASE + ".application.service.."
        );

        DomainIsolationTest violatingRules = new DomainIsolationTest();

        assertThrows(AssertionError.class, violatingRules::coreApplicationLayerShouldHaveNoFrameworkDependencies);
        assertDoesNotThrow(violatingRules::applicationServicesShouldNotCarrySpringStereotypes);
    }

    @Test
    void domainIsolationShouldFailServiceStereotypeRuleWhenApplicationServiceIsAnnotatedWithService() {
        configure(
                DOMAIN_SERVICE_STEREOTYPE_BASE,
                DOMAIN_SERVICE_STEREOTYPE_BASE + ".application.port.in..",
                DOMAIN_SERVICE_STEREOTYPE_BASE + ".application.port.out..",
                DOMAIN_SERVICE_STEREOTYPE_BASE + ".domain.model..",
                DOMAIN_SERVICE_STEREOTYPE_BASE + ".adapters..",
                DOMAIN_SERVICE_STEREOTYPE_BASE + ".application.service.."
        );

        DomainIsolationTest rules = new DomainIsolationTest();

        assertDoesNotThrow(rules::domainModelShouldOnlyDependOnJavaCoreAndDomainModel);
        assertDoesNotThrow(rules::coreApplicationLayerShouldHaveNoFrameworkDependencies);
        assertThrows(AssertionError.class, rules::applicationServicesShouldNotCarrySpringStereotypes);
    }

    @Test
    void dependencyDirectionShouldFailCoreRuleWhenCoreDependsOnAdapters() {
        configure(
                DEPENDENCY_CORE_BASE,
                DEPENDENCY_CORE_BASE + ".application.port.in..",
                DEPENDENCY_CORE_BASE + ".application.port.out..",
                DEPENDENCY_CORE_BASE + ".domain.model..",
                DEPENDENCY_CORE_BASE + ".adapters..",
                DEPENDENCY_CORE_BASE + ".application.service.."
        );

        DependencyDirectionTest rules = new DependencyDirectionTest();

        assertThrows(AssertionError.class, rules::coreApplicationLayerShouldNotDependOnAdapters);
        assertDoesNotThrow(rules::adaptersShouldNotDependOnServiceImplementations);
        assertDoesNotThrow(rules::onlyConfigurationMayDependOnServiceImplementations);
    }

    @Test
    void dependencyDirectionShouldFailCoreRuleWhenOnlySplitAdapterPropertiesAreConfigured() {
        configure(
                DEPENDENCY_CORE_BASE,
                DEPENDENCY_CORE_BASE + ".application.port.in..",
                DEPENDENCY_CORE_BASE + ".application.port.out..",
                DEPENDENCY_CORE_BASE + ".domain.model..",
                "",
                DEPENDENCY_CORE_BASE + ".application.service.."
        );
        // Deliberately leave the legacy ADAPTERS_KEY empty (set above) and only configure the
        // split inboundAdapters property, mirroring a consumer who follows the Architecture
        // Validator plugin's preferred split layout without also setting the legacy aggregate.
        System.setProperty(INBOUND_ADAPTERS_KEY, DEPENDENCY_CORE_BASE + ".adapters..");
        System.setProperty(OUTBOUND_ADAPTERS_KEY, "");

        DependencyDirectionTest rules = new DependencyDirectionTest();

        // Regression test: RulePackConfiguration.adapters() must also honor inboundAdapters/
        // outboundAdapters, not only the legacy aggregate architectureValidator.adapters
        // property; otherwise this violation would pass vacuously for split-layout consumers.
        assertThrows(AssertionError.class, rules::coreApplicationLayerShouldNotDependOnAdapters);
    }

    @Test
    void dependencyDirectionShouldFailAdapterRuleWhenAdapterDependsOnServiceImplementation() {
        configure(
                DEPENDENCY_ADAPTER_BASE,
                DEPENDENCY_ADAPTER_BASE + ".application.port.in..",
                DEPENDENCY_ADAPTER_BASE + ".application.port.out..",
                DEPENDENCY_ADAPTER_BASE + ".domain.model..",
                DEPENDENCY_ADAPTER_BASE + ".application.domain.service.adapter..",
                DEPENDENCY_ADAPTER_BASE + ".application.domain.service.impl.."
        );

        DependencyDirectionTest rules = new DependencyDirectionTest();

        assertDoesNotThrow(rules::coreApplicationLayerShouldNotDependOnAdapters);
        assertThrows(AssertionError.class, rules::adaptersShouldNotDependOnServiceImplementations);
        assertDoesNotThrow(rules::onlyConfigurationMayDependOnServiceImplementations);
    }

    @Test
    void dependencyDirectionShouldFailConfigurationRuleWhenNonConfigurationDependsOnServiceImplementation() {
        configure(
                DEPENDENCY_NON_CONFIG_BASE,
                DEPENDENCY_NON_CONFIG_BASE + ".application.port.in..",
                DEPENDENCY_NON_CONFIG_BASE + ".application.port.out..",
                DEPENDENCY_NON_CONFIG_BASE + ".domain.model..",
                DEPENDENCY_NON_CONFIG_BASE + ".adapters..",
                DEPENDENCY_NON_CONFIG_BASE + ".application.service.."
        );

        DependencyDirectionTest rules = new DependencyDirectionTest();

        assertDoesNotThrow(rules::coreApplicationLayerShouldNotDependOnAdapters);
        assertDoesNotThrow(rules::adaptersShouldNotDependOnServiceImplementations);
        assertThrows(AssertionError.class, rules::onlyConfigurationMayDependOnServiceImplementations);
    }

    @Test
    void portContractShouldFailInputPortRuleWhenInputPortIsAConcreteClass() {
        configure(
                PORT_INPUT_BASE,
                PORT_INPUT_BASE + ".application.port.in..",
                PORT_INPUT_BASE + ".application.port.out..",
                PORT_INPUT_BASE + ".domain.model..",
                PORT_INPUT_BASE + ".adapters..",
                PORT_INPUT_BASE + ".application.service.."
        );

        PortContractTest rules = new PortContractTest();

        assertThrows(AssertionError.class, rules::inputPortsShouldBeInterfaces);
        assertDoesNotThrow(rules::outputPortsShouldBeInterfaces);
        assertDoesNotThrow(rules::portsShouldOnlyDependOnJavaCoreAndDomainModel);
    }

    @Test
    void portContractShouldFailOutputPortRuleWhenOutputPortIsAConcreteClass() {
        configure(
                PORT_OUTPUT_BASE,
                PORT_OUTPUT_BASE + ".application.port.in..",
                PORT_OUTPUT_BASE + ".application.port.out..",
                PORT_OUTPUT_BASE + ".domain.model..",
                PORT_OUTPUT_BASE + ".adapters..",
                PORT_OUTPUT_BASE + ".application.service.."
        );

        PortContractTest rules = new PortContractTest();

        assertDoesNotThrow(rules::inputPortsShouldBeInterfaces);
        assertThrows(AssertionError.class, rules::outputPortsShouldBeInterfaces);
        assertDoesNotThrow(rules::portsShouldOnlyDependOnJavaCoreAndDomainModel);
    }

    @Test
    void portContractShouldFailSignatureRuleWhenPortExposesFrameworkType() {
        configure(
                PORT_SIGNATURE_BASE,
                PORT_SIGNATURE_BASE + ".application.port.in..",
                PORT_SIGNATURE_BASE + ".application.port.out..",
                PORT_SIGNATURE_BASE + ".domain.model..",
                PORT_SIGNATURE_BASE + ".adapters..",
                PORT_SIGNATURE_BASE + ".application.service.."
        );

        PortContractTest rules = new PortContractTest();

        assertDoesNotThrow(rules::inputPortsShouldBeInterfaces);
        assertDoesNotThrow(rules::outputPortsShouldBeInterfaces);
        assertThrows(AssertionError.class, rules::portsShouldOnlyDependOnJavaCoreAndDomainModel);
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
            String applicationServices
    ) {
        System.setProperty(BASE_PACKAGE_KEY, basePackage);
        System.setProperty(IN_PORTS_KEY, inPorts);
        System.setProperty(OUT_PORTS_KEY, outPorts);
        System.setProperty(DOMAIN_MODEL_KEY, domainModel);
        System.setProperty(ADAPTERS_KEY, adapters);
        System.setProperty(APPLICATION_SERVICES_KEY, applicationServices);
    }
}
