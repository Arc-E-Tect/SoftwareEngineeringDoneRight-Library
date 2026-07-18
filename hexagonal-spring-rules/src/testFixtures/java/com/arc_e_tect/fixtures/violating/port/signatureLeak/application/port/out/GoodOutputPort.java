package com.arc_e_tect.fixtures.violating.port.signatureLeak.application.port.out;

import com.arc_e_tect.fixtures.violating.port.signatureLeak.domain.model.PortDomain;

public interface GoodOutputPort {
    PortDomain load(String id);
}
