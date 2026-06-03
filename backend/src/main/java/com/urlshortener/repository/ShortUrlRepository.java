package com.urlshortener.repository;

import com.urlshortener.model.ShortUrl;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, UUID> {

    Optional<ShortUrl> findByShortCodeAndActiveTrue(String shortCode);

    boolean existsByShortCodeIgnoreCase(String shortCode);

    @Query("""
        select s from ShortUrl s
        where s.owner.id = :ownerId
          and (
            :search is null
            or lower(s.shortCode) like lower(concat('%', :search, '%'))
            or lower(s.originalUrl) like lower(concat('%', :search, '%'))
            or lower(coalesce(s.title, '')) like lower(concat('%', :search, '%'))
          )
          and (
            :status = 'ALL'
            or (:status = 'ACTIVE' and s.active = true and (s.expiresAt is null or s.expiresAt > CURRENT_TIMESTAMP))
            or (:status = 'EXPIRED' and s.expiresAt is not null and s.expiresAt <= CURRENT_TIMESTAMP)
            or (:status = 'INACTIVE' and s.active = false)
          )
        """)
    Page<ShortUrl> searchOwnedUrls(@Param("ownerId") UUID ownerId, @Param("search") String search, @Param("status") String status, Pageable pageable);

    long countByOwnerId(UUID ownerId);

    @Query("""
        select count(s) from ShortUrl s
        where s.owner.id = :ownerId
          and s.active = true
          and (s.expiresAt is null or s.expiresAt > CURRENT_TIMESTAMP)
        """)
    long countActiveByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("""
        select count(s) from ShortUrl s
        where s.owner.id = :ownerId
          and s.expiresAt is not null
          and s.expiresAt <= CURRENT_TIMESTAMP
        """)
    long countExpiredByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("select coalesce(sum(s.clickCount), 0) from ShortUrl s where s.owner.id = :ownerId")
    long totalClicksByOwnerId(@Param("ownerId") UUID ownerId);
}
