package com.hoaitran.shortlink.repository;

import com.hoaitran.shortlink.entity.UrlLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UrlLinkRepository extends JpaRepository<UrlLink, Long> {
    Optional<UrlLink> findByShortCode(String shortCode);
    boolean existsByShortCode(String shortCode);
}
