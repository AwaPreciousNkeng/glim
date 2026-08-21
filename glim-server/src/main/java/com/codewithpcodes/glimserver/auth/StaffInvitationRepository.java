package com.codewithpcodes.glimserver.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StaffInvitationRepository extends JpaRepository<StaffInvitation, UUID> {
    Optional<StaffInvitation> findByTokenHash(String tokenHash);
}
