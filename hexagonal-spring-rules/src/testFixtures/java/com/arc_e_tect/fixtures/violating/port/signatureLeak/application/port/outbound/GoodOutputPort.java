package com.arc_e_tect.fixtures.violating.port.signatureLeak.application.port.outbound;

import com.arc_e_tect.fixtures.violating.port.signatureLeak.domain.model.PortDomain;

public interface GoodOutputPort {
    PortDomain load(String id);
}
