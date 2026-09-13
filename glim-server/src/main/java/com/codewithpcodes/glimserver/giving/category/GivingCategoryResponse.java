package com.codewithpcodes.glimserver.giving.category;

import java.util.UUID;

public record GivingCategoryResponse(
        UUID id,
        String code,
        String name,
        String description,
        long minimumAmount,
        boolean requiresPartnership,
        String iconName
) {
    public static GivingCategoryResponse from(GivingCategory givingCategory, String language) {
        boolean french = "FRENCH".equalsIgnoreCase(language);
        return new GivingCategoryResponse(
                givingCategory.getId(),
                givingCategory.getCode(),
                french ? givingCategory.getNameFr() : givingCategory.getNameEn(),
                french ? givingCategory.getDescriptionFr() : givingCategory.getDescriptionEn(),
                givingCategory.getMinimumAmount() == null ? 0 : givingCategory.getMinimumAmount(),
                givingCategory.isRequiresPartnership(),
                givingCategory.getIconName()
        );
    }
}
