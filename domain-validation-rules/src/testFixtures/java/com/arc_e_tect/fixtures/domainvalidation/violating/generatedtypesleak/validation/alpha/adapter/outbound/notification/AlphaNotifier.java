package com.arc_e_tect.fixtures.domainvalidation.violating.generatedtypesleak.validation.alpha.adapter.outbound.notification;

import com.arc_e_tect.fixtures.domainvalidation.violating.generatedtypesleak.validation.alpha.adapter.inbound.web.api.AlphaResponse;

/** An outbound adapter that reuses a type generated for the inbound web adapter. */
public class AlphaNotifier {

    public String notify(AlphaResponse response) {
        return response.outcome();
    }
}
