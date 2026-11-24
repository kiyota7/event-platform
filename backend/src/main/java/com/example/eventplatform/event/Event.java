package com.example.eventplatform.event;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Event {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long organizerId;
    @Column(nullable = false)
    private String title;
    @Column(length = 4000)
    private String description;
    private String location;
    /** 日時はUTCのInstantで保存し、表示用にイベントのタイムゾーンを保持する */
    @Column(nullable = false)
    private Instant startAt;
    @Column(nullable = false)
    private Instant endAt;
    @Column(nullable = false)
    private String timeZone;
    @Column(nullable = false)
    private int capacity;
    /** 申込の楽観的ロック用(#3で利用) */
    @Version
    private long version;
    /** 確定済み参加者数(定員管理。version更新と同一トランザクションで更新) */
    @Column(nullable = false)
    private int confirmedCount = 0;
    @ManyToOne
    private Category category;
    @ManyToMany
    private Set<Tag> tags = new HashSet<>();
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Event() {}
    public Event(Long organizerId) { this.organizerId = organizerId; }

    public Long getId() { return id; }
    public Long getOrganizerId() { return organizerId; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { description = v; }
    public String getLocation() { return location; }
    public void setLocation(String v) { location = v; }
    public Instant getStartAt() { return startAt; }
    public void setStartAt(Instant v) { startAt = v; }
    public Instant getEndAt() { return endAt; }
    public void setEndAt(Instant v) { endAt = v; }
    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String v) { timeZone = v; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int v) { capacity = v; }
    public long getVersion() { return version; }
    public int getConfirmedCount() { return confirmedCount; }
    public void setConfirmedCount(int v) { confirmedCount = v; }
    public Category getCategory() { return category; }
    public void setCategory(Category c) { category = c; }
    public Set<Tag> getTags() { return tags; }
    public void setTags(Set<Tag> t) { tags = t; }
}
