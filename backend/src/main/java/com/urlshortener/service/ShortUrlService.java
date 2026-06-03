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
import com.urlshortener.model.UrlStatus;
import com.urlshortener.repository.ShortCodeSequenceRepository;
import com.urlshortener.repository.ShortUrlRepository;
import com.urlshortener.util.Base62Encoder;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
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
    private static final Duration DEFAULT_CACHE_TTL = Duration.ofHours(24);

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
            .status(UrlStatus.ACTIVE)
            .clickCount(0L)
            .active(true)
            .build();
        shortUrl.setStatus(resolveEffectiveStatus(shortUrl));
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
        String normalizedSearch = normalizeSearch(search);
        String normalizedStatus = normalizeStatus(status);
        Page<ShortUrl> result = normalizedSearch == null
            ? shortUrlRepository.findOwnedUrls(user.getId(), normalizedStatus, pageable)
            : shortUrlRepository.searchOwnedUrls(user.getId(), "%" + normalizedSearch.toLowerCase(Locale.ROOT) + "%", normalizedStatus, pageable);
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
            if (Boolean.TRUE.equals(request.active()) && isExpired(shortUrl)) {
                throw new BadRequestException("URL has expired");
            }
            shortUrl.setActive(request.active());
            shortUrl.setStatus(request.active() ? UrlStatus.ACTIVE : UrlStatus.INACTIVE);
        }
        shortUrl.setStatus(resolveEffectiveStatus(shortUrl));
        ShortUrl saved = shortUrlRepository.save(shortUrl);
        cacheShortUrl(saved);
        return shortUrlMapper.toResponse(saved);
    }

    @Transactional
    public void deactivateOwnedUrl(AppUser user, UUID shortUrlId) {
        ShortUrl shortUrl = findOwnedUrl(user, shortUrlId);
        applyInactive(shortUrl);
    }

    @Transactional
    public ShortUrlResponse deactivateOwnedUrlAndReturn(AppUser user, UUID shortUrlId) {
        ShortUrl shortUrl = findOwnedUrl(user, shortUrlId);
        applyInactive(shortUrl);
        return shortUrlMapper.toResponse(shortUrl);
    }

    @Transactional
    public ShortUrlResponse activateOwnedUrl(AppUser user, UUID shortUrlId) {
        ShortUrl shortUrl = findOwnedUrl(user, shortUrlId);
        if (isExpired(shortUrl)) {
            shortUrl.setStatus(UrlStatus.EXPIRED);
            shortUrlRepository.save(shortUrl);
            evictCache(shortUrl.getShortCode());
            throw new BadRequestException("URL has expired");
        }
        shortUrl.setActive(true);
        shortUrl.setStatus(UrlStatus.ACTIVE);
        ShortUrl saved = shortUrlRepository.save(shortUrl);
        cacheShortUrl(saved);
        return shortUrlMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ShortUrl getActiveShortUrl(String shortCode) {
        return shortUrlRepository.findByShortCodeIgnoreCase(normalizeShortCode(shortCode))
            .filter(shortUrl -> resolveEffectiveStatus(shortUrl) == UrlStatus.ACTIVE)
            .orElseThrow(() -> new ResourceNotFoundException("Short URL not found or expired"));
    }

    @Transactional(readOnly = true)
    public CachedShortUrl resolveCachedShortUrl(String shortCode) {
        String key = cacheKey(shortCode);
        String payload = stringRedisTemplate.opsForValue().get(key);
        if (payload != null) {
            try {
                return objectMapper.readValue(payload, CachedShortUrl.class);
            } catch (JsonProcessingException ignored) {
            }
        }

        ShortUrl shortUrl = shortUrlRepository.findByShortCodeIgnoreCase(normalizeShortCode(shortCode))
            .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
        cacheShortUrl(shortUrl);
        return new CachedShortUrl(
            shortUrl.getShortCode(),
            shortUrl.getOriginalUrl(),
            shortUrl.getExpiresAt(),
            resolveEffectiveStatus(shortUrl).name(),
            shortUrl.isActive()
        );
    }

    public void cacheShortUrl(ShortUrl shortUrl) {
        try {
            CachedShortUrl cachedShortUrl = new CachedShortUrl(
                shortUrl.getShortCode(),
                shortUrl.getOriginalUrl(),
                shortUrl.getExpiresAt(),
                resolveEffectiveStatus(shortUrl).name(),
                shortUrl.isActive()
            );
            Duration ttl = cacheTtl(shortUrl.getExpiresAt());
            stringRedisTemplate.opsForValue().set(cacheKey(shortUrl.getShortCode()), objectMapper.writeValueAsString(cachedShortUrl), ttl);
        } catch (JsonProcessingException ignored) {
        }
    }

    public void evictCache(String shortCode) {
        stringRedisTemplate.delete(cacheKey(shortCode));
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
            String normalizedAlias = normalizeShortCode(customAlias);
            if (shortUrlRepository.existsByShortCodeIgnoreCase(normalizedAlias)) {
                throw new ConflictException("Custom alias is already in use");
            }
            return normalizedAlias;
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
        return resolveEffectiveStatus(shortUrl) == UrlStatus.ACTIVE;
    }

    public UrlStatus resolveStatus(CachedShortUrl cachedShortUrl) {
        if (cachedShortUrl.expiresAt() != null && cachedShortUrl.expiresAt().isBefore(OffsetDateTime.now())) {
            return UrlStatus.EXPIRED;
        }
        if (cachedShortUrl.status() == null || cachedShortUrl.status().isBlank()) {
            return cachedShortUrl.active() ? UrlStatus.ACTIVE : UrlStatus.INACTIVE;
        }
        return UrlStatus.valueOf(cachedShortUrl.status());
    }

    @Transactional
    public int markExpiredUrls() {
        return shortUrlRepository.markExpiredUrls(UrlStatus.EXPIRED);
    }

    private void applyInactive(ShortUrl shortUrl) {
        shortUrl.setActive(false);
        shortUrl.setStatus(isExpired(shortUrl) ? UrlStatus.EXPIRED : UrlStatus.INACTIVE);
        shortUrlRepository.save(shortUrl);
        evictCache(shortUrl.getShortCode());
    }

    private UrlStatus resolveEffectiveStatus(ShortUrl shortUrl) {
        if (isExpired(shortUrl)) {
            return UrlStatus.EXPIRED;
        }
        if (shortUrl.getStatus() == UrlStatus.INACTIVE || !shortUrl.isActive()) {
            return UrlStatus.INACTIVE;
        }
        return UrlStatus.ACTIVE;
    }

    private boolean isExpired(ShortUrl shortUrl) {
        return shortUrl.getExpiresAt() != null && !shortUrl.getExpiresAt().isAfter(OffsetDateTime.now());
    }

    private String cacheKey(String shortCode) {
        return CACHE_PREFIX + normalizeShortCode(shortCode);
    }

    private Duration cacheTtl(OffsetDateTime expiresAt) {
        if (expiresAt == null) {
            return DEFAULT_CACHE_TTL;
        }
        Duration ttl = Duration.between(OffsetDateTime.now(), expiresAt);
        return ttl.isNegative() || ttl.isZero() ? Duration.ofMinutes(1) : ttl;
    }

    private String normalizeShortCode(String shortCode) {
        return shortCode.trim().toLowerCase(Locale.ROOT);
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
