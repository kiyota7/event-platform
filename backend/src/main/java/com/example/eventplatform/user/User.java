package com.example.eventplatform.user;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String passwordHash;
    @Column(nullable = false)
    private String displayName;
    /** ユーザーの表示用タイムゾーン(IANA形式) */
    @Column(nullable = false)
    private String timeZone = "Asia/Tokyo";
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected User() {}
    public User(String email, String passwordHash, String displayName, String timeZone) {
        this.email = email; this.passwordHash = passwordHash;
        this.displayName = displayName;
        if (timeZone != null) this.timeZone = timeZone;
    }
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getTimeZone() { return timeZone; }
}
