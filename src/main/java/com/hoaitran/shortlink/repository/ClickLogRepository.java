package com.hoaitran.shortlink.repository;

import com.hoaitran.shortlink.entity.ClickLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClickLogRepository extends JpaRepository<ClickLog, Long> {
}
