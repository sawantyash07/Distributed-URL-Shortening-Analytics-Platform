package com.urlshortener.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "normalizedDatabaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String datasourceUrl = firstPresent(
                environment.getProperty("JDBC_DATABASE_URL"),
                environment.getProperty("SPRING_DATASOURCE_URL"),
                environment.getProperty("DATABASE_URL"));

        NormalizedDatabaseUrl normalized = normalize(datasourceUrl);
        String normalizedUrl = normalized.url();
        if (!StringUtils.hasText(normalizedUrl)) {
            return;
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.datasource.url", normalizedUrl);
        if (StringUtils.hasText(normalized.username())
                && !StringUtils.hasText(environment.getProperty("SPRING_DATASOURCE_USERNAME"))) {
            properties.put("spring.datasource.username", normalized.username());
        }
        if (StringUtils.hasText(normalized.password())
                && !StringUtils.hasText(environment.getProperty("SPRING_DATASOURCE_PASSWORD"))) {
            properties.put("spring.datasource.password", normalized.password());
        }
        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private NormalizedDatabaseUrl normalize(String url) {
        if (!StringUtils.hasText(url)) {
            return NormalizedDatabaseUrl.empty();
        }
        if (url.startsWith("jdbc:postgresql://")) {
            return normalizePostgresUri(url.substring("jdbc:".length()));
        }
        if (url.startsWith("postgresql://")) {
            return normalizePostgresUri(url);
        }
        if (url.startsWith("postgres://")) {
            return normalizePostgresUri("postgresql://" + url.substring("postgres://".length()));
        }
        if (url.startsWith("jdbc:")) {
            return new NormalizedDatabaseUrl(url, null, null);
        }
        return new NormalizedDatabaseUrl(url, null, null);
    }

    private NormalizedDatabaseUrl normalizePostgresUri(String url) {
        URI uri = URI.create(url);
        StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://").append(uri.getHost());
        if (uri.getPort() > 0) {
            jdbcUrl.append(":").append(uri.getPort());
        }
        jdbcUrl.append(uri.getRawPath());
        if (StringUtils.hasText(uri.getRawQuery())) {
            jdbcUrl.append("?").append(uri.getRawQuery());
        }

        String username = null;
        String password = null;
        String userInfo = uri.getRawUserInfo();
        if (StringUtils.hasText(userInfo)) {
            String[] parts = userInfo.split(":", 2);
            username = decode(parts[0]);
            if (parts.length > 1) {
                password = decode(parts[1]);
            }
        }

        return new NormalizedDatabaseUrl(jdbcUrl.toString(), username, password);
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private record NormalizedDatabaseUrl(String url, String username, String password) {
        private static NormalizedDatabaseUrl empty() {
            return new NormalizedDatabaseUrl(null, null, null);
        }
    }
}
