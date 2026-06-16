package com.hoaitran.shortlink.repository;

import com.hoaitran.shortlink.entity.ClickEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
    Page<ClickEvent> findByLinkId(Long linkId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM ClickEvent c WHERE c.link.id IN (SELECT l.id FROM Link l WHERE l.expiresAt < :now)")
    void deleteByLinkExpiresAtBefore(@Param("now") LocalDateTime now);
}

