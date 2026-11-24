package com.example.eventplatform.dashboard;

import com.example.eventplatform.registration.Registration.Status;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class DashboardDtos {
    private DashboardDtos() {}

    public record EventStats(Long eventId, String title, Instant startAt, String timeZone, int capacity,
                             long confirmed, long waitlisted, long canceled, double fillRate) {}

    public record Summary(int totalEvents, int upcomingEvents, long totalConfirmed, long totalWaitlisted,
                          double averageFillRate, List<EventStats> events) {}

    public record Participant(Long userId, String displayName, String email, Status status, Instant registeredAt) {}

    public record DailyCount(LocalDate date, long registrations) {}

    /** trendはイベントのタイムゾーン基準の日別・新規申込数 */
    public record EventDetail(EventStats stats, List<DailyCount> trend, List<Participant> participants) {}
}
