package com.hoaitran.shortlink.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hoaitran.shortlink.entity.Link;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface LinkRepository extends JpaRepository<Link, Long> {
    Optional<Link> findByShortCode(String shortCode);
    boolean existsByShortCode(String shortCode);

    @Modifying
    @Query("DELETE FROM Link l WHERE l.expiresAt < :now")
    void deleteExpiredLinks(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Link l SET l.clickCount = l.clickCount + 1 WHERE l.id = :id")
    void incrementClickCount(@Param("id") Long id);
}
