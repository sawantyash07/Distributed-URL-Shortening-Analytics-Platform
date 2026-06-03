package com.urlshortener.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String frontendUrl;
    private String shortBaseUrl;
    private Jwt jwt = new Jwt();
    private RateLimits rateLimits = new RateLimits();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long accessTokenMinutes;
    }

    @Getter
    @Setter
    public static class RateLimits {
        private int authPerMinute;
        private int createPerMinute;
        private int redirectPerMinute;
    }
}
