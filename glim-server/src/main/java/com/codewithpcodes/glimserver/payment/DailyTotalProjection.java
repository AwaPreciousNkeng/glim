package com.codewithpcodes.glimserver.payment;

import java.time.Instant;

public interface DailyTotalProjection {
    Instant getDay();
    long getTotal();
}
