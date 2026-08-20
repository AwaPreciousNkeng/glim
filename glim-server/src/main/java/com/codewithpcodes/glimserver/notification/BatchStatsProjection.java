package com.codewithpcodes.glimserver.notification;

import java.time.Instant;
import java.util.UUID;

public interface BatchStatsProjection {
    UUID getBatchId();
    String getType();
    Instant getSentAt();
    long getRecipients();
    long getSent();
    long getFailed();
    long getNoDevice();
    long getOptedOut();
}
