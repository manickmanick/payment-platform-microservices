package com.ecommerce.user_service.dto;

public record AuthResponse(
        Long userId,
        String name,
        String email,
        String tokenType,
        String accessToken,
        long expiresIn
) {
}