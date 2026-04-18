package com.hoaitran.shortlink.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "url_links", indexes = @Index(name = "idx_short_code", columnList = "shortCode"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrlLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2048)
    private String originalUrl;

    @Column(nullable = false, unique = true, length = 10)
    private String shortCode;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    @Builder.Default
    private boolean isActive = true;
}
