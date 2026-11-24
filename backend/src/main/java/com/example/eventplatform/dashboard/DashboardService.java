package com.example.eventplatform.dashboard;

import com.example.eventplatform.dashboard.DashboardDtos.*;
import com.example.eventplatform.event.Event;
import com.example.eventplatform.event.EventRepository;
import com.example.eventplatform.registration.Registration;
import com.example.eventplatform.registration.Registration.Status;
import com.example.eventplatform.registration.RegistrationRepository;
import com.example.eventplatform.user.User;
import com.example.eventplatform.user.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class DashboardService {
    private final EventRepository events;
    private final RegistrationRepository registrations;
    private final UserRepository users;

    public DashboardService(EventRepository events, RegistrationRepository registrations, UserRepository users) {
        this.events = events; this.registrations = registrations; this.users = users;
    }

    public Summary summary(Long organizerId) {
        List<Event> mine = events.findByOrganizerIdOrderByStartAtDesc(organizerId);
        Map<Long, Map<Status, Long>> counts = new HashMap<>();
        if (!mine.isEmpty()) {
            for (Object[] row : registrations.countByStatusForEvents(mine.stream().map(Event::getId).toList()))
                counts.computeIfAbsent((Long) row[0], k -> new EnumMap<>(Status.class)).put((Status) row[1], (Long) row[2]);
        }
        List<EventStats> stats = mine.stream().map(e -> stats(e, counts.getOrDefault(e.getId(), Map.of()))).toList();
        Instant now = Instant.now();
        return new Summary(mine.size(), (int) mine.stream().filter(e -> e.getStartAt().isAfter(now)).count(),
                stats.stream().mapToLong(EventStats::confirmed).sum(), stats.stream().mapToLong(EventStats::waitlisted).sum(),
                round(stats.stream().mapToDouble(EventStats::fillRate).average().orElse(0)), stats);
    }

    public EventDetail detail(Long organizerId, Long eventId) {
        Event e = events.findById(eventId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "event not found"));
        if (!e.getOrganizerId().equals(organizerId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not the organizer");
        List<Registration> regs = registrations.findByEventIdOrderByIdAsc(eventId);
        Map<Status, Long> counts = regs.stream().collect(Collectors.groupingBy(Registration::getStatus, () -> new EnumMap<>(Status.class), Collectors.counting()));
        Map<Long, User> userMap = users.findAllById(regs.stream().map(Registration::getUserId).toList())
                .stream().collect(Collectors.toMap(User::getId, u -> u));
        ZoneId zone = ZoneId.of(e.getTimeZone());
        Map<LocalDate, Long> daily = new TreeMap<>();
        for (Registration r : regs) daily.merge(r.getCreatedAt().atZone(zone).toLocalDate(), 1L, Long::sum);
        List<Participant> participants = regs.stream().filter(r -> r.getStatus() != Status.CANCELED).map(r -> {
            User u = userMap.get(r.getUserId());
            return new Participant(r.getUserId(), u == null ? "(退会済み)" : u.getDisplayName(), u == null ? "" : u.getEmail(),
                    r.getStatus(), r.getCreatedAt());
        }).toList();
        return new EventDetail(stats(e, counts), daily.entrySet().stream().map(x -> new DailyCount(x.getKey(), x.getValue())).toList(), participants);
    }

    private EventStats stats(Event e, Map<Status, Long> c) {
        long confirmed = c.getOrDefault(Status.CONFIRMED, 0L);
        return new EventStats(e.getId(), e.getTitle(), e.getStartAt(), e.getTimeZone(), e.getCapacity(), confirmed,
                c.getOrDefault(Status.WAITLISTED, 0L), c.getOrDefault(Status.CANCELED, 0L),
                e.getCapacity() == 0 ? 0 : round(100.0 * confirmed / e.getCapacity()));
    }

    private static double round(double v) { return Math.round(v * 10) / 10.0; }
}
