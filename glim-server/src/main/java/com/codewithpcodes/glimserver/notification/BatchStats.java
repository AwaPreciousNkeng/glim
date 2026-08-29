package com.codewithpcodes.glimserver.notification;

import java.time.Instant;
import java.util.UUID;

public record BatchStats(
        UUID batchId,
        String type,
        Instant sentAt,
        long recipients,
        long sent,
        long failed,
        long noDevice,
        long optedOut,
        double deliveryRate,
        double reachRate
) {
    public static BatchStats from(BatchStatsProjection projection) {
        long attempted = projection.getSent() + projection.getFailed();

        //How many messages attempted were actually accepted
        double delivery = attempted == 0 ? 0 : (double) (projection.getSent() / attempted) * 100;

        double reach = projection.getRecipients() == 0 ? 0 : (double) projection.getSent() / projection.getRecipients() * 100;

        return new BatchStats(
                projection.getBatchId(),
                projection.getType(),
                projection.getSentAt(),
                projection.getRecipients(),
                projection.getSent(),
                projection.getFailed(),
                projection.getNoDevice(),
                projection.getOptedOut(),
                round(delivery),
                round(reach)
        );

    }

    private static double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
