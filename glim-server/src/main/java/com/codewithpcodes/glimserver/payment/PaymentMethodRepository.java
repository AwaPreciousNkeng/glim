package com.codewithpcodes.glimserver.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {
    List<PaymentMethod> findByUserIdOrderByIsDefaultDescCreatedAtDesc(UUID userId);
    Optional<PaymentMethod> findByUserIdAndMsisdn(UUID userId, String msisdn);

}
