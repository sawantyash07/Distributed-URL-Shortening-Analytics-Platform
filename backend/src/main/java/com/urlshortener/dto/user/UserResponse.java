package com.urlshortener.dto.user;

import com.urlshortener.model.Role;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record UserResponse(
    UUID id,
    String email,
    String fullName,
    Role role,
    OffsetDateTime createdAt
) {
}
