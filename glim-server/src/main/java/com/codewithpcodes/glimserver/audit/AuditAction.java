package com.codewithpcodes.glimserver.audit;

public final class AuditAction {

    private AuditAction() {}

    // giving
    public static final String TRANSACTION_MANUAL_RESOLVE = "TRANSACTION_MANUAL_RESOLVE";
    public static final String TRANSACTION_REVERIFY       = "TRANSACTION_REVERIFY";
    public static final String TRANSACTION_CASH_ENTRY     = "TRANSACTION_CASH_ENTRY";
    public static final String TRANSACTION_REVERSED       = "TRANSACTION_REVERSED";
    public static final String GIVING_DATA_EXPORTED       = "GIVING_DATA_EXPORTED";
    public static final String MEMBER_GIVING_VIEWED       = "MEMBER_GIVING_VIEWED";
    public static final String CATEGORY_CHANGED           = "CATEGORY_CHANGED";

    // access
    public static final String ROLE_CHANGED       = "ROLE_CHANGED";
    public static final String USER_DEACTIVATED   = "USER_DEACTIVATED";
    public static final String SESSIONS_REVOKED   = "SESSIONS_REVOKED";

    // content
    public static final String BROADCAST_SENT      = "BROADCAST_SENT";
    public static final String TESTIMONY_MODERATED = "TESTIMONY_MODERATED";

    // partnership
    public static final String PARTNER_IMPORT       = "PARTNER_IMPORT";
    public static final String PARTNER_CLAIM_APPROVED = "PARTNER_CLAIM_APPROVED";
}
