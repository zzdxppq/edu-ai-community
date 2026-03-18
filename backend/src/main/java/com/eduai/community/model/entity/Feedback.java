package com.eduai.community.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "feedbacks")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long messageId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String reasons;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false)
    private Boolean escalated;

    @Column(length = 20)
    private String escalateStatus;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (escalated == null) {
            escalated = false;
        }
    }

    public enum EscalateStatus {
        PENDING,
        PROCESSING,
        COMPLETED
    }
}
