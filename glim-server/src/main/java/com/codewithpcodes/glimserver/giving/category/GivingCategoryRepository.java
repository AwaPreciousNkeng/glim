package com.codewithpcodes.glimserver.giving.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GivingCategoryRepository extends JpaRepository<GivingCategory, UUID> {
    Optional<GivingCategory> findByCode(String code);
    List<GivingCategory> findByActiveTrueOrderByDisplayOrderAsc();
}
