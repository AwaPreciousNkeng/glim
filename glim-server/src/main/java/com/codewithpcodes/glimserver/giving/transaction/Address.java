package com.codewithpcodes.glimserver.giving.transaction;

public record Address(
        String city,
        String country,
        String line1,
        String line2,
        String postalCode,
        String state
) {
}
