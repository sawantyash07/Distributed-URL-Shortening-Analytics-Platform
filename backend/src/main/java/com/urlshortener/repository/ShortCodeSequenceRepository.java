package com.urlshortener.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ShortCodeSequenceRepository {

    private final JdbcTemplate jdbcTemplate;

    public long nextValue() {
        Long next = jdbcTemplate.queryForObject("select nextval('short_url_sequence')", Long.class);
        return next == null ? System.currentTimeMillis() : next;
    }
}
