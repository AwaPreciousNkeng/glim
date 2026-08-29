package com.codewithpcodes.glimserver.payment;

public enum TransactionSource {
    /** Member paid in the app. */
    ONLINE,
    /** Finance officer recorded a cash gift. */
    MANUAL_ENTRY,
    /** Imported from the paper partner list. */
    MIGRATED
}
