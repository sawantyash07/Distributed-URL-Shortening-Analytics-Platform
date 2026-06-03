package com.urlshortener.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.dto.common.PagedResponse;
import com.urlshortener.dto.url.BulkCreateShortUrlRequest;
import com.urlshortener.dto.url.CachedShortUrl;
import com.urlshortener.dto.url.CreateShortUrlRequest;
import com.urlshortener.dto.url.ShortUrlResponse;
import com.urlshortener.dto.url.UpdateShortUrlRequest;
import com.urlshortener.exception.BadRequestException;
import com.urlshortener.exception.ConflictException;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.exception.UnauthorizedOperationException;
import com.urlshortener.model.AppUser;
import com.urlshortener.model.ShortUrl;
import com.urlshortener.repository.ShortCodeSequenceRepository;
import com.urlshortener.repository.ShortUrlRepository;
import com.urlshortener.util.Base62Encoder;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShortUrlService {

    private static final String CACHE_PREFIX = "short-url:";

    private final ShortUrlRepository shortUrlRepository;
    private final ShortCodeSequenceRepository shortCodeSequenceRepository;
    private final ShortUrlMapper shortUrlMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public ShortUrlResponse createShortUrl(AppUser user, CreateShortUrlRequest request) {
        validateExpiration(request.expiresAt());
        String shortCode = resolveShortCode(request.customAlias());
        ShortUrl shortUrl = ShortUrl.builder()
            .id(UUID.randomUUID())
            .owner(user)
            .shortCode(shortCode)
            .customAlias(request.customAlias() != null && !request.customAlias().isBlank())
            .title(request.title())
            .originalUrl(request.originalUrl())
            .expiresAt(request.expiresAt())
            .clickCount(0L)
            .active(true)
            .build();
        ShortUrl saved = shortUrlRepository.save(shortUrl);
        cacheShortUrl(saved);
        return shortUrlMapper.toResponse(saved);
    }

    @Transactional
    public List<ShortUrlResponse> createBulk(AppUser user, BulkCreateShortUrlRequest request) {
        return request.urls().stream().map(item -> createShortUrl(user, item)).toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<ShortUrlResponse> listUrls(AppUser user, int page, int size, String search, String status, String sortBy, String direction) {
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ShortUrl> result = shortUrlRepository.searchOwnedUrls(user.getId(), normalizeSearch(search), normalizeStatus(status), pageable);
        return PagedResponse.<ShortUrlResponse>builder()
            .content(result.getContent().stream().map(shortUrlMapper::toResponse).toList())
            .page(result.getNumber())
            .size(result.getSize())
            .totalElements(result.getTotalElements())
            .totalPages(result.getTotalPages())
            .first(result.isFirst())
            .last(result.isLast())
            .build();
    }

    @Transactional(readOnly = true)
    public ShortUrlResponse getOwnedUrl(AppUser user, UUID shortUrlId) {
        return shortUrlMapper.toResponse(findOwnedUrl(user, shortUrlId));
    }

    @Transactional
    public ShortUrlResponse updateOwnedUrl(AppUser user, UUID shortUrlId, UpdateShortUrlRequest request) {
        ShortUrl shortUrl = findOwnedUrl(user, shortUrlId);
        if (request.expiresAt() != null) {
            validateExpiration(request.expiresAt());
            shortUrl.setExpiresAt(request.expiresAt());
        }
        if (request.title() != null) {
            shortUrl.setTitle(request.title().trim());
        }
        if (request.active() != null) {
            shortUrl.setActive(request.active());
        }
        ShortUrl saved = shortUrlRepository.save(shortUrl);
        cacheShortUrl(saved);
        return shortUrlMapper.toResponse(saved);
    }

    @Transactional
    public void deactivateOwnedUrl(AppUser user, UUID shortUrlId) {
        ShortUrl shortUrl = findOwnedUrl(user, shortUrlId);
        shortUrl.setActive(false);
        shortUrlRepository.save(shortUrl);
        evictCache(shortUrl.getShortCode());
    }

    @Transactional(readOnly = true)
    public ShortUrl getActiveShortUrl(String shortCode) {
        return shortUrlRepository.findByShortCodeAndActiveTrue(shortCode)
            .filter(this::isUsable)
            .orElseThrow(() -> new ResourceNotFoundException("Short URL not found or expired"));
    }

    @Transactional(readOnly = true)
    public CachedShortUrl resolveCachedShortUrl(String shortCode) {
        String key = CACHE_PREFIX + shortCode;
        String payload = stringRedisTemplate.opsForValue().get(key);
        if (payload != null) {
            try {
                CachedShortUrl cached = objectMapper.readValue(payload, CachedShortUrl.class);
                if (cached.active() && (cached.expiresAt() == null || cached.expiresAt().isAfter(OffsetDateTime.now()))) {
                    return cached;
                }
            } catch (JsonProcessingException ignored) {
            }
        }

        ShortUrl shortUrl = getActiveShortUrl(shortCode);
        cacheShortUrl(shortUrl);
        return new CachedShortUrl(shortUrl.getShortCode(), shortUrl.getOriginalUrl(), shortUrl.getExpiresAt(), shortUrl.isActive());
    }

    public void cacheShortUrl(ShortUrl shortUrl) {
        try {
            CachedShortUrl cachedShortUrl = new CachedShortUrl(
                shortUrl.getShortCode(),
                shortUrl.getOriginalUrl(),
                shortUrl.getExpiresAt(),
                shortUrl.isActive()
            );
            stringRedisTemplate.opsForValue().set(CACHE_PREFIX + shortUrl.getShortCode(), objectMapper.writeValueAsString(cachedShortUrl));
        } catch (JsonProcessingException ignored) {
        }
    }

    public void evictCache(String shortCode) {
        stringRedisTemplate.delete(CACHE_PREFIX + shortCode);
    }

    private ShortUrl findOwnedUrl(AppUser user, UUID shortUrlId) {
        ShortUrl shortUrl = shortUrlRepository.findById(shortUrlId)
            .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
        if (!shortUrl.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedOperationException("You can only manage your own URLs");
        }
        return shortUrl;
    }

    private String resolveShortCode(String customAlias) {
        if (customAlias != null && !customAlias.isBlank()) {
            if (shortUrlRepository.existsByShortCodeIgnoreCase(customAlias)) {
                throw new ConflictException("Custom alias is already in use");
            }
            return customAlias;
        }
        String code = Base62Encoder.encode(shortCodeSequenceRepository.nextValue());
        if (shortUrlRepository.existsByShortCodeIgnoreCase(code)) {
            throw new ConflictException("Generated short code already exists; please retry");
        }
        return code;
    }

    private void validateExpiration(OffsetDateTime expiresAt) {
        if (expiresAt != null && expiresAt.isBefore(OffsetDateTime.now().plusMinutes(1))) {
            throw new BadRequestException("Expiration must be at least one minute in the future");
        }
    }

    private boolean isUsable(ShortUrl shortUrl) {
        return shortUrl.isActive() && (shortUrl.getExpiresAt() == null || shortUrl.getExpiresAt().isAfter(OffsetDateTime.now()));
    }

    private String normalizeSearch(String search) {
        return search == null || search.isBlank() ? null : search.trim();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "ALL";
        }
        String normalized = status.trim().toUpperCase();
        return switch (normalized) {
            case "ACTIVE", "EXPIRED", "INACTIVE", "ALL" -> normalized;
            default -> throw new BadRequestException("Unsupported status filter");
        };
    }
}
