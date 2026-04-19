package com.hoaitran.shortlink.repository;

import com.hoaitran.shortlink.entity.ClickLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClickLogRepository extends JpaRepository<ClickLog, Long> {
    java.util.List<ClickLog> findByUrlLinkShortCode(String shortCode);
    
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByUrlLinkId(Long urlLinkId);
}
