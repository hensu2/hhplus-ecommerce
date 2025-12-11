package com.hhplus.ecommerce.domain.event;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "failed_events")
public class FailedEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "event_payload", columnDefinition = "TEXT", nullable = false)
    private String eventPayload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FailedEventStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Long createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    public FailedEventEntity(
        EventType eventType,
        Long orderId,
        String eventPayload,
        String errorMessage
    ) {
        this.eventType = eventType;
        this.orderId = orderId;
        this.eventPayload = eventPayload;
        this.errorMessage = errorMessage;
        this.retryCount = 0;
        this.status = FailedEventStatus.PENDING;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void increaseRetryCount() {
        this.retryCount++;
        this.updatedAt = System.currentTimeMillis();
    }

    public void markAsProcessing() {
        this.status = FailedEventStatus.PROCESSING;
        this.updatedAt = System.currentTimeMillis();
    }

    public void markAsSuccess() {
        this.status = FailedEventStatus.SUCCESS;
        this.updatedAt = System.currentTimeMillis();
    }

    public void markAsFailed(String errorMessage) {
        this.status = FailedEventStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = System.currentTimeMillis();
    }
}