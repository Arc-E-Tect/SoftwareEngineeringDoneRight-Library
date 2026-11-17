package com.arc_e_tect.fixtures.domainvalidation.violating.mutablecommand.validation.beta.configuration;

import com.arc_e_tect.fixtures.domainvalidation.violating.mutablecommand.validation.beta.application.domain.service.ValidateBetaService;
import com.arc_e_tect.fixtures.domainvalidation.violating.mutablecommand.validation.beta.application.port.inbound.ValidateBetaUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class BetaConfiguration {

    @Bean
    ValidateBetaUseCase validateBetaUseCase() {
        return new ValidateBetaService();
    }
}
