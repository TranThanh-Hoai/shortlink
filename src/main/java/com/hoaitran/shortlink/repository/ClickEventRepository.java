package com.hoaitran.shortlink.repository;

import com.hoaitran.shortlink.entity.ClickEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
    Page<ClickEvent> findByLinkId(Long linkId, Pageable pageable);
}
