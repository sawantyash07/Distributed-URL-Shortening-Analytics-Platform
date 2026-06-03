package com.urlshortener.dto.auth;

import com.urlshortener.dto.user.UserResponse;
import lombok.Builder;

@Builder
public record AuthResponse(
    String accessToken,
    String tokenType,
    long expiresInMinutes,
    UserResponse user
) {
}
