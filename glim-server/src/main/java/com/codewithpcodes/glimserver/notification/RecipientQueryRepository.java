package com.codewithpcodes.glimserver.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RecipientQueryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public List<NotificationDispatcher.Recipient> resolve(Audience audience, NotificationType type) {

        String preferenceColumn = preferenceColumnFor(type);

        StringBuilder sql = new StringBuilder("""
            SELECT u.id, u.preferred_language, u.phone
            FROM users u
            LEFT JOIN notification_preferences p ON p.user_id = u.id
            WHERE u.status = 'ACTIVE'
            """);

        // COALESCE(..., true): no preference row means the member has never
        // changed settings, which means everything is on.
        if (preferenceColumn != null) {
            sql.append(" AND COALESCE(p.").append(preferenceColumn).append(", true) = true");
        }

        var params = new MapSqlParameterSource();

        switch (audience.type()) {
            case MINISTRY -> {
                sql.append(" AND EXISTS (SELECT 1 FROM ministry_members m " +
                        "WHERE m.user_id = u.id AND m.ministry_id = :ref)");
                params.addValue("ref", audience.reference());
            }
            case PARTNERS_ACTIVE -> sql.append(
                    " AND EXISTS (SELECT 1 FROM partnerships pa " +
                            "WHERE pa.user_id = u.id AND pa.state IN ('ACTIVE','GRACE'))");
            case SINGLE_USER -> {
                sql.append(" AND u.id = :ref");
                params.addValue("ref", audience.reference());
            }
            case ALL_MEMBERS -> { }
        }

        return jdbc.query(sql.toString(), params, (rs, i) -> new NotificationDispatcher.Recipient(
                UUID.fromString(rs.getString("id")),
                rs.getString("preferred_language"),
                rs.getString("phone")));
    }

    public boolean isTypeEnabled(UUID userId, NotificationType type) {
        String column = preferenceColumnFor(type);
        if (column == null) return true;

        String sql = "SELECT COALESCE(p." + column + ", true) FROM users u " +
                "LEFT JOIN notification_preferences p ON p.user_id = u.id WHERE u.id = :id";

        return Boolean.TRUE.equals(jdbc.queryForObject(sql,
                new MapSqlParameterSource("id", userId), Boolean.class));
    }

    private String preferenceColumnFor(NotificationType type) {
        return switch (type) {
            case PARTNERSHIP_REMINDER, PARTNERSHIP_LAPSED -> "partnership_reminders";
            case NEW_ANNOUNCEMENT -> "announcements";
            case DAILY_DEVOTIONAL -> "devotionals";
            case SERVICE_LIVE, EVENT_REMINDER -> "events";
            default -> null;
        };
    }
}
