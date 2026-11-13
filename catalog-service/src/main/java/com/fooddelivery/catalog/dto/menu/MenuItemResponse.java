package com.fooddelivery.catalog.dto.menu;

import java.math.BigDecimal;

public record MenuItemResponse(

        Long id,
        Long restaurantId,
        String name,
        BigDecimal price,
        Boolean available
) {
}
