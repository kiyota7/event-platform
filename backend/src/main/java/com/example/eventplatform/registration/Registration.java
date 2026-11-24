package com.example.eventplatform.registration;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"eventId", "userId"}))
public class Registration {
    public enum Status { CONFIRMED, WAITLISTED, CANCELED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long eventId;
    @Column(nullable = false)
    private Long userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Status status;
    /** キャンセル待ちの順序用(再申込時に更新) */
    @Column(nullable = false)
    private long queueOrder;
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
    /** 初回申込日時(推移統計用。再申込でも変えない) */
    private Instant createdAt = Instant.now();
    /** リマインダー送信済み日時(未送信はnull) */
    private Instant reminderSentAt;

    protected Registration() {}
    public Registration(Long eventId, Long userId) { this.eventId = eventId; this.userId = userId; }

    public Long getId() { return id; }
    public Long getEventId() { return eventId; }
    public Long getUserId() { return userId; }
    public Status getStatus() { return status; }
    public long getQueueOrder() { return queueOrder; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setStatus(Status s) { status = s; updatedAt = Instant.now(); }
    public void setQueueOrder(long q) { queueOrder = q; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReminderSentAt() { return reminderSentAt; }
    public void setReminderSentAt(Instant t) { reminderSentAt = t; }
}
