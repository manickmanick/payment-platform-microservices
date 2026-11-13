package com.fooddelivery.catalog.dto.menu;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record MenuItemBulkRequest(

        @NotEmpty(message = "Menu item IDs cannot be empty")
        List<@NotNull Long> menuItemIds
) {
}
