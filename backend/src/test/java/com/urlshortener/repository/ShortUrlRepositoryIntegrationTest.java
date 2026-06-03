package com.urlshortener.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.urlshortener.model.AppUser;
import com.urlshortener.model.Role;
import com.urlshortener.model.ShortUrl;
import com.urlshortener.model.UrlStatus;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ShortUrlRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @Test
    void shouldSearchOwnedUrls() {
        AppUser user = appUserRepository.save(AppUser.builder()
            .id(UUID.randomUUID())
            .email("owner@example.com")
            .passwordHash("hash")
            .fullName("Owner")
            .role(Role.USER)
            .active(true)
            .build());

        shortUrlRepository.save(ShortUrl.builder()
            .id(UUID.randomUUID())
            .owner(user)
            .shortCode("abc123")
            .originalUrl("https://example.com/docs")
            .title("Docs")
            .status(UrlStatus.ACTIVE)
            .expiresAt(OffsetDateTime.now().plusDays(1))
            .active(true)
            .clickCount(4)
            .build());

        var result = shortUrlRepository.searchOwnedUrls(user.getId(), "%docs%", "ALL", org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getShortCode()).isEqualTo("abc123");
    }

    @Test
    void shouldFilterByLifecycleStatuses() {
        AppUser user = appUserRepository.save(AppUser.builder()
            .id(UUID.randomUUID())
            .email("filters@example.com")
            .passwordHash("hash")
            .fullName("Filters")
            .role(Role.USER)
            .active(true)
            .build());

        shortUrlRepository.save(ShortUrl.builder()
            .id(UUID.randomUUID())
            .owner(user)
            .shortCode("active1")
            .originalUrl("https://example.com/active")
            .title("Active")
            .status(UrlStatus.ACTIVE)
            .expiresAt(OffsetDateTime.now().plusDays(1))
            .active(true)
            .clickCount(1)
            .build());

        shortUrlRepository.save(ShortUrl.builder()
            .id(UUID.randomUUID())
            .owner(user)
            .shortCode("inactive1")
            .originalUrl("https://example.com/inactive")
            .title("Inactive")
            .status(UrlStatus.INACTIVE)
            .expiresAt(OffsetDateTime.now().plusDays(1))
            .active(false)
            .clickCount(0)
            .build());

        shortUrlRepository.save(ShortUrl.builder()
            .id(UUID.randomUUID())
            .owner(user)
            .shortCode("expired1")
            .originalUrl("https://example.com/expired")
            .title("Expired")
            .status(UrlStatus.EXPIRED)
            .expiresAt(OffsetDateTime.now().minusDays(1))
            .active(true)
            .clickCount(8)
            .build());

        var pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        assertThat(shortUrlRepository.findOwnedUrls(user.getId(), "ACTIVE", pageable).getTotalElements()).isEqualTo(1);
        assertThat(shortUrlRepository.findOwnedUrls(user.getId(), "INACTIVE", pageable).getTotalElements()).isEqualTo(1);
        assertThat(shortUrlRepository.findOwnedUrls(user.getId(), "EXPIRED", pageable).getTotalElements()).isEqualTo(1);
    }
}
