package com.codewithpcodes.glimserver.notification.push;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {
    List<DeviceToken> findByUserIdIn(Collection<UUID> userIds);

    List<DeviceToken> findByUserId(UUID userId);

    void deleteByFcmToken(String fcmToken);

    /**
     * A token can move between users (shared phone, someone signs out and
     * a family member signs in). ON CONFLICT reassigns rather than failing.
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO device_tokens (id, user_id, fcm_token, platform, device_model, app_version,
                                   failure_count, created_at, last_used_at)
        VALUES (gen_random_uuid(), :userId, :token, :platform, :model, :version, 0, now(), now())
        ON CONFLICT (fcm_token) DO UPDATE
        SET user_id = :userId,
            platform = :platform,
            device_model = :model,
            app_version = :version,
            failure_count = 0,
            last_used_at = now()
        """, nativeQuery = true)
    void upsert(@Param("userId") UUID userId,
                @Param("token") String token,
                @Param("platform") String platform,
                @Param("model") String model,
                @Param("version") String version);
}
