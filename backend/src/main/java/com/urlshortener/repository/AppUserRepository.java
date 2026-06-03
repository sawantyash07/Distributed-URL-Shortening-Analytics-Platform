package com.urlshortener.repository;

import com.urlshortener.model.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query(value = """
        select *
        from app_user
        where lower(email) = lower(:email)
          and password_hash = crypt(:password, password_hash)
        limit 1
        """, nativeQuery = true)
    Optional<AppUser> authenticate(@Param("email") String email, @Param("password") String password);
}
