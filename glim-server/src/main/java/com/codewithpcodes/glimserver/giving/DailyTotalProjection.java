package com.codewithpcodes.glimserver.giving;

import java.time.Instant;

public interface DailyTotalProjection {
    Instant getDay();
    long getTotal();
}
