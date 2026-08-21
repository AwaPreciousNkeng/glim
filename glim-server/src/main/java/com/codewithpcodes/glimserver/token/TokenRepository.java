package com.codewithpcodes.glimserver.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TokenRepository extends JpaRepository<Token, Integer> {
    @Query(value = "select t from Token t inner join User u " +
            "on t.user.id = u.id " +
            "where u.id = :id and (t.expired = false or t.revoked = false )")
    List<Token> findAllValidTokenByUser(UUID userID);
    Optional<Token> findByToken(String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM Token t WHERE t.expired < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}

