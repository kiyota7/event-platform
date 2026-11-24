package com.example.eventplatform.event;

import java.time.Instant;
import java.util.List;

/** from(含む)〜to(含まない)は開始日時に対する範囲 */
public record EventSearchCriteria(String q, Long categoryId, List<String> tags, Instant from, Instant to,
                                  boolean availableOnly, Long organizerId) {}
