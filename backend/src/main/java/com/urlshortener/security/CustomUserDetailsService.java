package com.urlshortener.security;

import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        return appUserRepository.findByEmailIgnoreCase(username)
            .map(UserPrincipal::new)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
