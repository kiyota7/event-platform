package com.example.eventplatform.dashboard;

import com.example.eventplatform.dashboard.DashboardDtos.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** 主催者専用。自分が主催したイベントの情報だけを返す */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService service;
    public DashboardController(DashboardService service) { this.service = service; }

    @GetMapping
    public Summary summary(Authentication a) { return service.summary((Long) a.getPrincipal()); }

    @GetMapping("/events/{id}")
    public EventDetail detail(Authentication a, @PathVariable Long id) { return service.detail((Long) a.getPrincipal(), id); }
}
