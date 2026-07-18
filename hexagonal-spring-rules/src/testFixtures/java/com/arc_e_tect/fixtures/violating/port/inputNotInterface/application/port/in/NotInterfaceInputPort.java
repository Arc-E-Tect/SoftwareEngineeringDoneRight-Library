package com.arc_e_tect.fixtures.violating.port.inputNotInterface.application.port.in;

import com.arc_e_tect.fixtures.violating.port.inputNotInterface.domain.model.PortDomain;

public class NotInterfaceInputPort {

    public PortDomain execute(PortDomain domain) {
        return domain;
    }
}
