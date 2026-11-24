package com.example.eventplatform.registration;

import com.example.eventplatform.registration.Registration.Status;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class RegistrationController {
    public record RegistrationResponse(Long id, Long eventId, Status status) {
        static RegistrationResponse of(Registration r) { return new RegistrationResponse(r.getId(), r.getEventId(), r.getStatus()); }
    }

    private final RegistrationService service;
    private final RegistrationRepository repo;

    public RegistrationController(RegistrationService service, RegistrationRepository repo) {
        this.service = service; this.repo = repo;
    }

    @PostMapping("/events/{eventId}/registrations") @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse register(Authentication a, @PathVariable Long eventId) {
        return RegistrationResponse.of(service.register(eventId, (Long) a.getPrincipal()));
    }

    @DeleteMapping("/events/{eventId}/registrations")
    public RegistrationResponse cancel(Authentication a, @PathVariable Long eventId) {
        return RegistrationResponse.of(service.cancel(eventId, (Long) a.getPrincipal()));
    }

    @GetMapping("/events/{eventId}/registrations/me")
    public RegistrationResponse mine(Authentication a, @PathVariable Long eventId) {
        return repo.findByEventIdAndUserId(eventId, (Long) a.getPrincipal()).map(RegistrationResponse::of)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/me/registrations")
    public List<RegistrationResponse> myRegistrations(Authentication a) {
        return repo.findByUserIdAndStatusNotOrderByIdDesc((Long) a.getPrincipal(), Status.CANCELED)
                .stream().map(RegistrationResponse::of).toList();
    }
}
