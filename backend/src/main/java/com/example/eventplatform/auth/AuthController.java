package com.example.eventplatform.auth;

import com.example.eventplatform.user.User;
import com.example.eventplatform.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.DateTimeException;
import java.time.ZoneId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public record RegisterRequest(@Email @NotBlank String email, @NotBlank @Size(min = 8, max = 72) String password,
                                  @NotBlank @Size(max = 50) String displayName, String timeZone) {}
    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    public record UserResponse(Long id, String email, String displayName, String timeZone) {
        static UserResponse of(User u) { return new UserResponse(u.getId(), u.getEmail(), u.getDisplayName(), u.getTimeZone()); }
    }
    public record AuthResponse(String token, UserResponse user) {}

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users; this.encoder = encoder; this.jwt = jwt;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (r.timeZone() != null) {
            try { ZoneId.of(r.timeZone()); }
            catch (DateTimeException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid timeZone"); }
        }
        if (users.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "email already registered");
        User u = users.save(new User(email, encoder.encode(r.password()), r.displayName(), r.timeZone()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(jwt.generate(u.getId(), u.getEmail()), UserResponse.of(u)));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) {
        User u = users.findByEmail(r.email().trim().toLowerCase())
                .filter(x -> encoder.matches(r.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials"));
        return new AuthResponse(jwt.generate(u.getId(), u.getEmail()), UserResponse.of(u));
    }

    @GetMapping("/me")
    public UserResponse me(Authentication auth) {
        Long id = (Long) auth.getPrincipal();
        return users.findById(id).map(UserResponse::of)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
