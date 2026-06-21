package com.urlshortener.service;

import com.urlshortener.config.AppProperties;
import com.urlshortener.dto.auth.AuthResponse;
import com.urlshortener.dto.auth.LoginRequest;
import com.urlshortener.dto.auth.RegisterRequest;
import com.urlshortener.exception.ConflictException;
import com.urlshortener.exception.UnauthorizedOperationException;
import com.urlshortener.model.AppUser;
import com.urlshortener.model.Role;
import com.urlshortener.repository.AppUserRepository;
import com.urlshortener.security.JwtService;
import com.urlshortener.security.UserPrincipal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppProperties appProperties;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (appUserRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("An account with this email already exists");
        }

        AppUser user = AppUser.builder()
            .id(UUID.randomUUID())
            .email(request.email().trim().toLowerCase())
            .passwordHash(passwordEncoder.encode(request.password()))
            .fullName(request.fullName().trim())
            .role(Role.USER)
            .active(true)
            .build();
        AppUser saved = appUserRepository.save(user);
        UserPrincipal principal = new UserPrincipal(saved);
        return buildAuthResponse(principal, saved);
    }

    public AuthResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(request.email().trim())
            .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
            .orElseThrow(() -> new UnauthorizedOperationException("Invalid email or password"));
        UserPrincipal principal = new UserPrincipal(user);
        return buildAuthResponse(principal, user);
    }

    private AuthResponse buildAuthResponse(UserPrincipal principal, AppUser user) {
        return AuthResponse.builder()
            .accessToken(jwtService.generateToken(principal))
            .tokenType("Bearer")
            .expiresInMinutes(appProperties.getJwt().getAccessTokenMinutes())
            .user(userMapper.toResponse(user))
            .build();
    }
}
