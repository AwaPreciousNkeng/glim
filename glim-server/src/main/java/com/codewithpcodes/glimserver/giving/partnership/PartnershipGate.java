package com.codewithpcodes.glimserver.giving.partnership;

import java.util.UUID;

public interface PartnershipGate {
    boolean hasPartnership(UUID userId);
}
