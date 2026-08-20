package com.codewithpcodes.glimserver.notification;

import java.time.Instant;
import java.util.UUID;

public record BatchStats(
        UUID batchId, String type, Instant sentAt,
        long recipients, long sent, long failed, long noDevice, long optedOut
) {
}
