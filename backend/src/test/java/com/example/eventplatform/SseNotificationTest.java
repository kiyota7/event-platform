package com.example.eventplatform;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.eventplatform.event.Event;
import com.example.eventplatform.event.EventRepository;
import com.example.eventplatform.notification.*;
import com.example.eventplatform.registration.RegistrationService;
import com.example.eventplatform.user.User;
import com.example.eventplatform.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@SpringBootTest
@AutoConfigureMockMvc
class SseNotificationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired UserRepository users;
    @Autowired EventRepository events;
    @Autowired RegistrationService registrations;
    @Autowired NotificationService notifications;
    @Autowired SseHub hub;

    private String register(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"password123\",\"displayName\":\"U\"}"))
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).get("token").asText();
    }

    @Test
    void ticketIsOneTimeAndRequiresAuth() throws Exception {
        String token = register("sse1@example.com");
        mvc.perform(post("/api/notifications/stream-ticket")).andExpect(status().isUnauthorized());
        String ticket = om.readTree(mvc.perform(post("/api/notifications/stream-ticket").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("ticket").asText();
        mvc.perform(get("/api/notifications/stream").param("ticket", ticket)).andExpect(request().asyncStarted());
        mvc.perform(get("/api/notifications/stream").param("ticket", ticket)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/notifications/stream").param("ticket", "bogus")).andExpect(status().isUnauthorized());
    }

    @Test
    void pushedToSubscriberAndListedAndMarkedRead() throws Exception {
        String token = register("sse2@example.com");
        Long uid = users.findByEmail("sse2@example.com").orElseThrow().getId();
        List<Object> received = new CopyOnWriteArrayList<>();
        hub.register(uid, new SseEmitter() {
            @Override public synchronized void send(SseEventBuilder b) throws IOException { b.build().forEach(d -> received.add(d.getData())); }
        });
        Event e = new Event(1L);
        e.setTitle("SSEテスト"); e.setTimeZone("Asia/Tokyo"); e.setCapacity(5);
        e.setStartAt(Instant.now().plus(100, ChronoUnit.HOURS)); e.setEndAt(Instant.now().plus(102, ChronoUnit.HOURS));
        e = events.save(e);

        registrations.register(e.getId(), uid);   // 非同期で通知が作られpushされる
        await().atMost(java.time.Duration.ofSeconds(5)).until(() -> received.stream().anyMatch(d -> d instanceof NotificationDto));
        NotificationDto dto = (NotificationDto) received.stream().filter(d -> d instanceof NotificationDto).findFirst().get();
        assertTrue(dto.message().contains("SSEテスト"));

        String auth = "Bearer " + token;
        mvc.perform(get("/api/notifications").header("Authorization", auth))
                .andExpect(jsonPath("$.unreadCount").value(1)).andExpect(jsonPath("$.items[0].read").value(false));
        mvc.perform(post("/api/notifications/" + dto.id() + "/read").header("Authorization", auth)).andExpect(status().isOk());
        mvc.perform(get("/api/notifications").header("Authorization", auth)).andExpect(jsonPath("$.unreadCount").value(0));

        String other = register("sse3@example.com");
        mvc.perform(post("/api/notifications/" + dto.id() + "/read").header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());
    }
}
