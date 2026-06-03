package com.urlshortener.service;

import com.urlshortener.dto.user.UserResponse;
import com.urlshortener.model.AppUser;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(AppUser user) {
        return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .role(user.getRole())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
