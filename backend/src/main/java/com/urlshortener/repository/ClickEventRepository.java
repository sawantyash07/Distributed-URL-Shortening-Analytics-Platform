package com.urlshortener.repository;

import com.urlshortener.model.ClickEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    @Query(value = """
        select cast(date(clicked_at) as date) as day, count(*) as count
        from click_event
        where short_url_id = :shortUrlId
        group by cast(date(clicked_at) as date)
        order by day
        """, nativeQuery = true)
    List<DailyClickProjection> findDailyClicks(@Param("shortUrlId") UUID shortUrlId);

    @Query(value = """
        select coalesce(browser, 'Unknown') as label, count(*) as count
        from click_event
        where short_url_id = :shortUrlId
        group by coalesce(browser, 'Unknown')
        order by count(*) desc
        limit 10
        """, nativeQuery = true)
    List<MetricBreakdownProjection> findBrowserBreakdown(@Param("shortUrlId") UUID shortUrlId);

    @Query(value = """
        select coalesce(operating_system, 'Unknown') as label, count(*) as count
        from click_event
        where short_url_id = :shortUrlId
        group by coalesce(operating_system, 'Unknown')
        order by count(*) desc
        limit 10
        """, nativeQuery = true)
    List<MetricBreakdownProjection> findOperatingSystemBreakdown(@Param("shortUrlId") UUID shortUrlId);

    @Query(value = """
        select coalesce(device_type, 'Unknown') as label, count(*) as count
        from click_event
        where short_url_id = :shortUrlId
        group by coalesce(device_type, 'Unknown')
        order by count(*) desc
        limit 10
        """, nativeQuery = true)
    List<MetricBreakdownProjection> findDeviceBreakdown(@Param("shortUrlId") UUID shortUrlId);

    @Query(value = """
        select coalesce(country, 'Unknown') as label, count(*) as count
        from click_event
        where short_url_id = :shortUrlId
        group by coalesce(country, 'Unknown')
        order by count(*) desc
        limit 10
        """, nativeQuery = true)
    List<MetricBreakdownProjection> findCountryBreakdown(@Param("shortUrlId") UUID shortUrlId);

    @Query(value = """
        select cast(date(c.clicked_at) as date) as day, count(*) as count
        from click_event c
        join short_url s on c.short_url_id = s.id
        where s.owner_id = :ownerId
        group by cast(date(c.clicked_at) as date)
        order by day
        """, nativeQuery = true)
    List<DailyClickProjection> findOwnerDailyClicks(@Param("ownerId") UUID ownerId);

    @Query(value = """
        select s.short_code as label, s.click_count as count
        from short_url s
        where s.owner_id = :ownerId
        order by s.click_count desc
        limit 5
        """, nativeQuery = true)
    List<MetricBreakdownProjection> findTopPerformingUrls(@Param("ownerId") UUID ownerId);

    interface DailyClickProjection {
        java.time.LocalDate getDay();
        long getCount();
    }

    interface MetricBreakdownProjection {
        String getLabel();
        long getCount();
    }
}
