package com.arc_e_tect.fixtures.violating.port.outputNotInterface.application.port.outbound;

import com.arc_e_tect.fixtures.violating.port.outputNotInterface.domain.model.PortDomain;

public class NotInterfaceOutputPort {

    public PortDomain load(String id) {
        return new PortDomain(id);
    }
}
