package com.arc_e_tect.fixtures.compliant.adapters.persistence;

import com.arc_e_tect.fixtures.compliant.application.port.outbound.OrderStorePort;
import org.springframework.stereotype.Repository;

@Repository
public class OrderStoreRepositoryAdapter implements OrderStorePort {

    @Override
    public void save() {
    }
}
