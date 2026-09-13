package com.codewithpcodes.glimserver.giving.config;

import com.codewithpcodes.glimserver.giving.category.GivingCategory;
import com.codewithpcodes.glimserver.giving.category.GivingCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GivingCategorySeeder implements ApplicationRunner {
    private final GivingCategoryRepository repository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        seed("TITHE", "Tithe", "Dîme", 1, "hand-coins", false, false);
        seed("OFFERING", "Offering", "Offrande", 2, "gift", false, false);
        seed("SEED", "Seed", "Semence", 3, "sprout", false, false);
        seed("BUILDING_FUND", "Building Fund", "Fonds de construction", 4, "building", false, false);
        seed("PARTNERSHIP", "Partnership", "Partenariat", 5, "handshake", true, true);
    }

    private void seed(String code, String english, String french, int order,
                      String icon, boolean requiresPartnership, boolean countsToward) {
        repository.findByCode(code)
                .orElseGet(() -> repository.save(
                        GivingCategory.builder()
                                .code(code)
                                .nameEn(english)
                                .nameFr(french)
                                .displayOrder(order)
                                .iconName(icon)
                                .requiresPartnership(requiresPartnership)
                                .countsTowardPartnership(countsToward)
                                .build()
                ));
    }
}
