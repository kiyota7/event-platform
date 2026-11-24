package com.example.eventplatform.event;

import com.example.eventplatform.event.EventDtos.*;
import com.example.eventplatform.registration.RegistrationRepository;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EventService {
    private final EventRepository events;
    private final CategoryRepository categories;
    private final TagRepository tags;
    private final RegistrationRepository registrations;

    public EventService(EventRepository events, CategoryRepository categories, TagRepository tags,
                        RegistrationRepository registrations) {
        this.events = events; this.categories = categories; this.tags = tags; this.registrations = registrations;
    }

    @Transactional
    public EventResponse create(Long organizerId, EventRequest r) {
        Event e = new Event(organizerId);
        apply(e, r);
        return toResponse(events.save(e));
    }

    @Transactional
    public EventResponse update(Long userId, Long id, EventRequest r) {
        Event e = find(id);
        requireOrganizer(e, userId);
        apply(e, r);
        return toResponse(e);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Event e = find(id);
        requireOrganizer(e, userId);
        registrations.deleteByEventId(id);
        events.delete(e);
    }

    @Transactional(readOnly = true)
    public EventResponse get(Long id) { return toResponse(find(id)); }

    @Transactional(readOnly = true)
    public Page<EventResponse> list(Pageable p) { return events.findAllByOrderByStartAtAsc(p).map(this::toResponse); }

    private Event find(Long id) {
        return events.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "event not found"));
    }

    private void requireOrganizer(Event e, Long userId) {
        if (!e.getOrganizerId().equals(userId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not the organizer");
    }

    private void apply(Event e, EventRequest r) {
        try { ZoneId.of(r.timeZone()); }
        catch (DateTimeException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid timeZone"); }
        if (!r.endAt().isAfter(r.startAt())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endAt must be after startAt");
        if (r.capacity() < e.getConfirmedCount())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "capacity is below confirmed registrations");
        e.setTitle(r.title()); e.setDescription(r.description()); e.setLocation(r.location());
        e.setStartAt(r.startAt().toInstant()); e.setEndAt(r.endAt().toInstant());
        e.setTimeZone(r.timeZone()); e.setCapacity(r.capacity());
        e.setCategory(r.categoryId() == null ? null : categories.findById(r.categoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "category not found")));
        Set<Tag> resolved = new HashSet<>();
        if (r.tags() != null) for (String raw : r.tags()) {
            String name = raw.trim().toLowerCase();
            resolved.add(tags.findByName(name).orElseGet(() -> tags.save(new Tag(name))));
        }
        e.setTags(resolved);
    }

    EventResponse toResponse(Event e) {
        ZoneId zone = ZoneId.of(e.getTimeZone());
        List<String> tagNames = e.getTags().stream().map(Tag::getName).sorted().toList();
        Category c = e.getCategory();
        return new EventResponse(e.getId(), e.getOrganizerId(), e.getTitle(), e.getDescription(), e.getLocation(),
                e.getStartAt(), e.getEndAt(), e.getTimeZone(),
                e.getStartAt().atZone(zone).toOffsetDateTime(), e.getEndAt().atZone(zone).toOffsetDateTime(),
                e.getCapacity(), e.getConfirmedCount(), c == null ? null : c.getId(), c == null ? null : c.getName(), tagNames);
    }
}
