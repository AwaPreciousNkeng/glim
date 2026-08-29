package com.codewithpcodes.glimserver.payment;

public interface CategoryTotalProjection {
    String getCategoryCode();
    long getCount();
    long getTotal();
}
