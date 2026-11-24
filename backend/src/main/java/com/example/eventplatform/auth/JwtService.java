package com.example.eventplatform.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generate(Long userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder().subject(String.valueOf(userId)).claim("email", email)
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expirationMinutes * 60)))
                .signWith(key).compact();
    }

    /** 検証してユーザーIDを返す。不正・期限切れなら例外 */
    public Long parseUserId(String token) throws JwtException {
        Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return Long.valueOf(c.getSubject());
    }
}
