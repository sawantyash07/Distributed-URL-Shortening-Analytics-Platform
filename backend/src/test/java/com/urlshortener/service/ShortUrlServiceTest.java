package com.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.dto.url.CreateShortUrlRequest;
import com.urlshortener.exception.ConflictException;
import com.urlshortener.model.AppUser;
import com.urlshortener.model.Role;
import com.urlshortener.model.ShortUrl;
import com.urlshortener.repository.ShortCodeSequenceRepository;
import com.urlshortener.repository.ShortUrlRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;
    @Mock
    private ShortCodeSequenceRepository shortCodeSequenceRepository;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private ShortUrlService shortUrlService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        shortUrlService = new ShortUrlService(shortUrlRepository, shortCodeSequenceRepository, new ShortUrlMapper(
            new com.urlshortener.config.AppProperties(),
            new com.urlshortener.util.QrCodeGenerator()
        ), stringRedisTemplate, new ObjectMapper());
    }

    @Test
    void shouldCreateGeneratedCodeWhenAliasMissing() {
        AppUser user = AppUser.builder().id(UUID.randomUUID()).email("user@example.com").fullName("User").role(Role.USER).active(true).build();
        CreateShortUrlRequest request = new CreateShortUrlRequest("Docs", "https://example.com/docs", null, OffsetDateTime.now().plusDays(1));

        when(shortCodeSequenceRepository.nextValue()).thenReturn(100000L);
        when(shortUrlRepository.existsByShortCodeIgnoreCase("Q0u")).thenReturn(false);
        when(shortUrlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = shortUrlService.createShortUrl(user, request);

        assertThat(response.shortCode()).isEqualTo("Q0u");
        assertThat(response.originalUrl()).isEqualTo("https://example.com/docs");
    }

    @Test
    void shouldRejectDuplicateCustomAlias() {
        AppUser user = AppUser.builder().id(UUID.randomUUID()).email("user@example.com").fullName("User").role(Role.USER).active(true).build();
        CreateShortUrlRequest request = new CreateShortUrlRequest("Docs", "https://example.com/docs", "myAlias", OffsetDateTime.now().plusDays(1));

        when(shortUrlRepository.existsByShortCodeIgnoreCase("myAlias")).thenReturn(true);

        assertThatThrownBy(() -> shortUrlService.createShortUrl(user, request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("already in use");
    }
}
