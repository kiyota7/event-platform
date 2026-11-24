package com.example.eventplatform.notification;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(indexes = @Index(columnList = "userId, id"))
public class Notification {
    public enum Type { REGISTRATION_CONFIRMED, PROMOTED_FROM_WAITLIST, REMINDER }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Type type;
    @Column(nullable = false)
    private String message;
    private Long eventId;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    private Instant readAt;

    protected Notification() {}
    public Notification(Long userId, Type type, String message, Long eventId) {
        this.userId = userId; this.type = type; this.message = message; this.eventId = eventId;
    }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Type getType() { return type; }
    public String getMessage() { return message; }
    public Long getEventId() { return eventId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant t) { readAt = t; }
}
