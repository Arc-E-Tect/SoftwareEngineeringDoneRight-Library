package com.arc_e_tect.fixtures.violating.port.outputNotInterface.application.port.inbound;

import com.arc_e_tect.fixtures.violating.port.outputNotInterface.domain.model.PortDomain;

public interface GoodInputPort {
    PortDomain execute(PortDomain domain);
}
