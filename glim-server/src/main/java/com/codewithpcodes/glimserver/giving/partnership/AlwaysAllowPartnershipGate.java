package com.codewithpcodes.glimserver.giving.partnership;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AlwaysAllowPartnershipGate implements PartnershipGate {

    @Override
    public boolean hasPartnership(UUID userId) {
        return true;
    }
}
