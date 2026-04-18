package com.hoaitran.shortlink.repository;

import com.hoaitran.shortlink.entity.UrlLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UrlLinkRepository extends JpaRepository<UrlLink, Long> {
    Optional<UrlLink> findByShortCode(String shortCode);
    Optional<UrlLink> findByOriginalUrl(String originalUrl);
    boolean existsByShortCode(String shortCode);

    @Modifying
    @Transactional
    @Query("UPDATE UrlLink u SET u.clickCount = u.clickCount + 1 WHERE u.shortCode = :shortCode")
    void incrementClickCount(String shortCode);

    java.util.List<UrlLink> findTop10ByOrderByClickCountDesc();
}
