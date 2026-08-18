package com.codewithpcodes.glimserver.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, UUID> {
    Optional<VerificationCode> findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
            UUID userId, CodePurpose purpose);

    @Query("SELECT COUNT(v) FROM VerificationCode v WHERE v.user.id = :userId " +
            "AND v.purpose = :purpose AND v.createdAt > :since")
    long countRecent(@Param("userId") UUID userId,
                     @Param("purpose") CodePurpose purpose,
                     @Param("since") Instant since);
}
