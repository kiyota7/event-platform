package com.example.eventplatform.event;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

final class EventSpecifications {
    private EventSpecifications() {}

    static Specification<Event> from(EventSearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (c.q() != null && !c.q().isBlank()) {
                String like = "%" + escape(c.q().trim().toLowerCase()) + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like, '\\'),
                        cb.like(cb.lower(cb.coalesce(root.get("description"), "")), like, '\\'),
                        cb.like(cb.lower(cb.coalesce(root.get("location"), "")), like, '\\')));
            }
            if (c.categoryId() != null) ps.add(cb.equal(root.get("category").get("id"), c.categoryId()));
            if (c.tags() != null) {
                // 指定タグをすべて持つイベント(AND)。タグごとにjoinする
                for (String raw : c.tags()) {
                    if (raw == null || raw.isBlank()) continue;
                    var join = root.join("tags");
                    ps.add(cb.equal(join.get("name"), raw.trim().toLowerCase()));
                }
            }
            if (c.from() != null) ps.add(cb.greaterThanOrEqualTo(root.get("startAt"), c.from()));
            if (c.to() != null) ps.add(cb.lessThan(root.get("startAt"), c.to()));
            if (c.availableOnly()) ps.add(cb.lessThan(root.get("confirmedCount"), root.<Integer>get("capacity")));
            if (c.organizerId() != null) ps.add(cb.equal(root.get("organizerId"), c.organizerId()));
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
