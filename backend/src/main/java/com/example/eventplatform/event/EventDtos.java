package com.example.eventplatform.event;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.List;

public final class EventDtos {
    private EventDtos() {}

    /** startAt/endAtはオフセット付きISO8601(例: 2026-12-01T19:00:00+09:00) */
    public record EventRequest(
            @NotBlank @Size(max = 100) String title,
            @Size(max = 4000) String description,
            @Size(max = 200) String location,
            @NotNull OffsetDateTime startAt,
            @NotNull OffsetDateTime endAt,
            @NotBlank String timeZone,
            @Min(1) @Max(100000) int capacity,
            Long categoryId,
            List<@NotBlank @Size(max = 30) String> tags) {}

    public record EventResponse(
            Long id, Long organizerId, String title, String description, String location,
            java.time.Instant startAt, java.time.Instant endAt, String timeZone,
            OffsetDateTime startLocal, OffsetDateTime endLocal,
            int capacity, Long categoryId, String categoryName, List<String> tags) {}

    public record CategoryRequest(@NotBlank @Size(max = 50) String name) {}
    public record NameResponse(Long id, String name) {}
}
