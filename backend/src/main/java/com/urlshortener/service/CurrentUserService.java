package com.urlshortener.service;

import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.model.AppUser;
import com.urlshortener.repository.AppUserRepository;
import com.urlshortener.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final AppUserRepository appUserRepository;

    public AppUser requireUser(UserPrincipal principal) {
        return appUserRepository.findByEmailIgnoreCase(principal.getUsername())
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user no longer exists"));
    }
}
