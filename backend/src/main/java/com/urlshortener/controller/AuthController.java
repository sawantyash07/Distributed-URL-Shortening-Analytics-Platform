package com.urlshortener.controller;

import com.urlshortener.dto.auth.AuthResponse;
import com.urlshortener.dto.auth.LoginRequest;
import com.urlshortener.dto.auth.RegisterRequest;
import com.urlshortener.dto.user.UserResponse;
import com.urlshortener.security.UserPrincipal;
import com.urlshortener.service.AuthService;
import com.urlshortener.service.CurrentUserService;
import com.urlshortener.service.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;
    private final UserMapper userMapper;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate an existing user")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user")
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return userMapper.toResponse(currentUserService.requireUser(principal));
    }
}
