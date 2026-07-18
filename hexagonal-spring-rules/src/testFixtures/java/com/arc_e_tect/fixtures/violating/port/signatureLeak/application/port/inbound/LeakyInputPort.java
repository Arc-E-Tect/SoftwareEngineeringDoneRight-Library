package com.arc_e_tect.fixtures.violating.port.signatureLeak.application.port.inbound;

import org.springframework.context.ApplicationContext;

public interface LeakyInputPort {
    void execute(ApplicationContext context);
}
