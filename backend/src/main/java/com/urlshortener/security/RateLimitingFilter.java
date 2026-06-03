package com.urlshortener.security;

import com.urlshortener.config.AppProperties;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final AppProperties appProperties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitingFilter(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        Bucket bucket = buckets.computeIfAbsent(buildKey(request), key -> createBucket(request));
        if (!bucket.tryConsume(1)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Too many requests. Please retry later.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String buildKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String identifier = forwarded == null ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
        return request.getRequestURI() + ":" + identifier;
    }

    private Bucket createBucket(HttpServletRequest request) {
        int capacity = switch (resolveCategory(request)) {
            case "AUTH" -> appProperties.getRateLimits().getAuthPerMinute();
            case "CREATE" -> appProperties.getRateLimits().getCreatePerMinute();
            default -> appProperties.getRateLimits().getRedirectPerMinute();
        };
        Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(capacity, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveCategory(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/auth")) {
            return "AUTH";
        }
        if (uri.startsWith("/api/urls")) {
            return "CREATE";
        }
        return "DEFAULT";
    }
}
