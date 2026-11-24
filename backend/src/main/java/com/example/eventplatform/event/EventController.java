package com.example.eventplatform.event;

import com.example.eventplatform.event.EventDtos.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    @GetMapping("/events")
    public Page<EventResponse> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.list(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
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
