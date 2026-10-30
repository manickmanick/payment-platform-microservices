package com.fooddelivery.catalog.dto.menu;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;


public record CreateMenuRequest(
        @NotNull
        Long restaurantId,

        @NotNull
        Long categoryId,

        @NotBlank
        @Size(max = 160)
        String name,

        @NotBlank
        @Size(max = 1000)
        String description,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal price,

        @NotBlank
        @Pattern(regexp = "VEG|NON_VEG")
        String foodType,

        @NotNull
        Boolean available
) {
}
