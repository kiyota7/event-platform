package com.example.eventplatform.event;

import com.example.eventplatform.event.EventDtos.*;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class EventController {
    private final EventService service;
    private final CategoryRepository categories;
    private final TagRepository tags;

    public EventController(EventService service, CategoryRepository categories, TagRepository tags) {
        this.service = service; this.categories = categories; this.tags = tags;
    }

    @PostMapping("/events") @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(Authentication a, @Valid @RequestBody EventRequest r) {
        return service.create((Long) a.getPrincipal(), r);
    }

    /** 検索・フィルタ。from/toはオフセット付きISO8601(URLでは+を%2Bにエンコード)。開始日時の昇順 */
    @GetMapping("/events")
    public Page<EventResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) List<String> tag,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "false") boolean availableOnly,
            @RequestParam(required = false) Long organizerId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var criteria = new EventSearchCriteria(q, categoryId, tag, from == null ? null : from.toInstant(),
                to == null ? null : to.toInstant(), availableOnly, organizerId);
        return service.search(criteria, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("startAt").ascending().and(Sort.by("id"))));
    }

    @GetMapping("/events/{id}")
    public EventResponse get(@PathVariable Long id) { return service.get(id); }

    @PutMapping("/events/{id}")
    public EventResponse update(Authentication a, @PathVariable Long id, @Valid @RequestBody EventRequest r) {
        return service.update((Long) a.getPrincipal(), id, r);
    }

    @DeleteMapping("/events/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication a, @PathVariable Long id) { service.delete((Long) a.getPrincipal(), id); }

    @GetMapping("/categories")
    public List<NameResponse> categories() {
        return categories.findAll().stream().map(c -> new NameResponse(c.getId(), c.getName())).toList();
    }

    @PostMapping("/categories") @ResponseStatus(HttpStatus.CREATED)
    public NameResponse createCategory(@Valid @RequestBody CategoryRequest r) {
        Category c = categories.save(new Category(r.name().trim()));
        return new NameResponse(c.getId(), c.getName());
    }

    @GetMapping("/tags")
    public List<NameResponse> tags() {
        return tags.findAll().stream().map(t -> new NameResponse(t.getId(), t.getName())).toList();
    }
}
