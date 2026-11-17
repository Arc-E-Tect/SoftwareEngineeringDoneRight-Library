package com.arc_e_tect.fixtures.domainvalidation.violating.centraldispatcher.validation.alpha.configuration;

import com.arc_e_tect.fixtures.domainvalidation.violating.centraldispatcher.validation.alpha.application.domain.service.ValidateAlphaService;
import com.arc_e_tect.fixtures.domainvalidation.violating.centraldispatcher.validation.alpha.application.port.inbound.ValidateAlphaUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AlphaConfiguration {

    @Bean
    ValidateAlphaUseCase validateAlphaUseCase() {
        return new ValidateAlphaService();
    }
}
