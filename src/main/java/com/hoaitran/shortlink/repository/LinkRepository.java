package com.hoaitran.shortlink.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hoaitran.shortlink.entity.Link;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface LinkRepository extends JpaRepository<Link, Long> {
    Optional<Link> findByShortCode(String shortCode);
    boolean existsByShortCode(String shortCode);
    void deleteAllByExpiresAtBefore(LocalDateTime now);
}
