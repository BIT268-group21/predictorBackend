package com.tradingapp.auth.dto;

public record AuthResponse(String token, String tokenType, long expiresInMs, UserResponse user) {
}
