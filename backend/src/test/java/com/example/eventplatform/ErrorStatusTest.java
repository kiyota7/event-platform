package com.example.eventplatform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

/**
 * 実サーバー(RANDOM_PORT)で、エラー応答が/error転送で401に化けないことを確認する。
 * MockMvcは/error転送を行わないため、この問題を検出できない。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:errordb;DB_CLOSE_DELAY=-1")
class ErrorStatusTest {
    @Autowired TestRestTemplate rest;

    @Test
    void errorStatusesArePreservedForAuthenticatedUser() {
        HttpHeaders json = new HttpHeaders();
        json.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"email\":\"err@example.com\",\"password\":\"password123\",\"displayName\":\"E\"}";
        ResponseEntity<String> reg = rest.postForEntity("/api/auth/register", new HttpEntity<>(body, json), String.class);
        assertEquals(HttpStatus.CREATED, reg.getStatusCode());
        String token = reg.getBody().replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        HttpHeaders auth = new HttpHeaders();
        auth.setBearerAuth(token);
        assertEquals(HttpStatus.NOT_FOUND, rest.exchange("/api/events/999/registrations/me", HttpMethod.GET, new HttpEntity<>(auth), String.class).getStatusCode());
        assertEquals(HttpStatus.CONFLICT, rest.postForEntity("/api/auth/register", new HttpEntity<>(body, json), String.class).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, rest.exchange("/api/dashboard/events/999", HttpMethod.GET, new HttpEntity<>(auth), String.class).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, rest.getForEntity("/api/notifications", String.class).getStatusCode());
    }
}
