package com.arc_e_tect.gradle.architecture.domainvalidation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("The controller rules")
class DomainValidationControllerSelfTest extends RuleSelfTestSupport {

    @Test
    @DisplayName("let a controller use its use case, its own web adapter, the execution component, the JDK and Spring")
    void controllerWithAllowedDependenciesPasses() {
        configure("compliant");

        assertDoesNotThrow(new DomainValidationControllerTest()::controllersDependOnlyOnUseCasesTheirAdapterAndExecution);
    }

    @Test
    @DisplayName("fail when a controller depends on a domain service")
    void controllerDependingOnADomainServiceFails() {
        configure("violating.controllerdependency");

        assertThrows(AssertionError.class, new DomainValidationControllerTest()::controllersDependOnlyOnUseCasesTheirAdapterAndExecution);
    }

    @Test
    @DisplayName("let a controller implement its slice's generated interface")
    void controllerImplementingTheGeneratedApiPasses() {
        configure("compliant");

        assertDoesNotThrow(new DomainValidationControllerTest()::controllersImplementAGeneratedApi);
    }

    @Test
    @DisplayName("fail when a controller implements no generated interface")
    void controllerWithoutGeneratedApiFails() {
        configure("violating.controllerwithoutapi");

        assertThrows(AssertionError.class, new DomainValidationControllerTest()::controllersImplementAGeneratedApi);
    }

    @Test
    @DisplayName("let a controller take its request mappings from the generated interface only")
    void controllerWithoutOwnMappingsPasses() {
        configure("compliant");

        assertDoesNotThrow(new DomainValidationControllerTest()::controllersDeclareNoRequestMappingsOfTheirOwn);
    }

    @Test
    @DisplayName("fail when a controller declares a request mapping of its own, such as a generic handler")
    void controllerWithOwnMappingFails() {
        configure("violating.controllerownmapping");

        assertThrows(AssertionError.class, new DomainValidationControllerTest()::controllersDeclareNoRequestMappingsOfTheirOwn);
    }

    @Test
    @DisplayName("let only the slice's inbound web adapter use its generated types")
    void generatedTypesInTheirAdapterPass() {
        configure("compliant");

        assertDoesNotThrow(new DomainValidationControllerTest()::generatedApiTypesStayInTheirInboundWebAdapter);
    }

    @Test
    @DisplayName("fail when an outbound adapter uses a generated type")
    void generatedTypeUsedByAnOutboundAdapterFails() {
        configure("violating.generatedtypesleak");

        assertThrows(AssertionError.class, new DomainValidationControllerTest()::generatedApiTypesStayInTheirInboundWebAdapter);
    }
}
