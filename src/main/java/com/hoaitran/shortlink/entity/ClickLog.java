package com.hoaitran.shortlink.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "click_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClickLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_link_id", nullable = false)
    private UrlLink urlLink;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime clickedAt;

    private String ipAddress;

    @Column(length = 1024)
    private String userAgent;

    @Column(length = 2048)
    private String referer;
}
