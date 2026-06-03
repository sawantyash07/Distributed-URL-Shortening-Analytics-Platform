package com.urlshortener.repository;

import com.urlshortener.model.ShortUrl;
import com.urlshortener.model.UrlStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, UUID> {

    Optional<ShortUrl> findByShortCodeIgnoreCase(String shortCode);

    boolean existsByShortCodeIgnoreCase(String shortCode);

    @Query("""
        select s from ShortUrl s
        where s.owner.id = :ownerId
          and (
            :status = 'ALL'
            or (:status = 'ACTIVE' and s.status = com.urlshortener.model.UrlStatus.ACTIVE and (s.expiresAt is null or s.expiresAt > CURRENT_TIMESTAMP))
            or (:status = 'EXPIRED' and (s.status = com.urlshortener.model.UrlStatus.EXPIRED or (s.expiresAt is not null and s.expiresAt <= CURRENT_TIMESTAMP)))
            or (:status = 'INACTIVE' and s.status = com.urlshortener.model.UrlStatus.INACTIVE and (s.expiresAt is null or s.expiresAt > CURRENT_TIMESTAMP))
          )
        """)
    Page<ShortUrl> findOwnedUrls(@Param("ownerId") UUID ownerId, @Param("status") String status, Pageable pageable);

    @Query("""
        select s from ShortUrl s
        where s.owner.id = :ownerId
          and (
            lower(s.shortCode) like :searchPattern
            or lower(s.originalUrl) like :searchPattern
            or lower(coalesce(s.title, '')) like :searchPattern
          )
          and (
            :status = 'ALL'
            or (:status = 'ACTIVE' and s.status = com.urlshortener.model.UrlStatus.ACTIVE and (s.expiresAt is null or s.expiresAt > CURRENT_TIMESTAMP))
            or (:status = 'EXPIRED' and (s.status = com.urlshortener.model.UrlStatus.EXPIRED or (s.expiresAt is not null and s.expiresAt <= CURRENT_TIMESTAMP)))
            or (:status = 'INACTIVE' and s.status = com.urlshortener.model.UrlStatus.INACTIVE and (s.expiresAt is null or s.expiresAt > CURRENT_TIMESTAMP))
          )
        """)
    Page<ShortUrl> searchOwnedUrls(@Param("ownerId") UUID ownerId, @Param("searchPattern") String searchPattern, @Param("status") String status, Pageable pageable);

    long countByOwnerId(UUID ownerId);

    @Query("""
        select count(s) from ShortUrl s
        where s.owner.id = :ownerId
          and s.status = com.urlshortener.model.UrlStatus.ACTIVE
          and (s.expiresAt is null or s.expiresAt > CURRENT_TIMESTAMP)
        """)
    long countActiveByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("""
        select count(s) from ShortUrl s
        where s.owner.id = :ownerId
          and s.status = com.urlshortener.model.UrlStatus.INACTIVE
          and (s.expiresAt is null or s.expiresAt > CURRENT_TIMESTAMP)
        """)
    long countInactiveByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("""
        select count(s) from ShortUrl s
        where s.owner.id = :ownerId
          and (s.status = com.urlshortener.model.UrlStatus.EXPIRED or (s.expiresAt is not null and s.expiresAt <= CURRENT_TIMESTAMP))
        """)
    long countExpiredByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("select coalesce(sum(s.clickCount), 0) from ShortUrl s where s.owner.id = :ownerId")
    long totalClicksByOwnerId(@Param("ownerId") UUID ownerId);

    @Modifying
    @Query("""
        update ShortUrl s
           set s.status = :expiredStatus
         where s.status <> :expiredStatus
           and s.expiresAt is not null
           and s.expiresAt <= CURRENT_TIMESTAMP
        """)
    int markExpiredUrls(@Param("expiredStatus") UrlStatus expiredStatus);
}
