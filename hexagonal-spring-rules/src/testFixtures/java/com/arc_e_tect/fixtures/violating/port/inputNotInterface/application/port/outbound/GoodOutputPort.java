package com.arc_e_tect.fixtures.violating.port.inputNotInterface.application.port.outbound;

import com.arc_e_tect.fixtures.violating.port.inputNotInterface.domain.model.PortDomain;

public interface GoodOutputPort {
    PortDomain load(String id);
}
