package com.example.eventplatform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.eventplatform.event.Event;
import com.example.eventplatform.event.EventRepository;
import com.example.eventplatform.registration.RegistrationService;
import com.example.eventplatform.user.User;
import com.example.eventplatform.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired UserRepository users;
    @Autowired EventRepository events;
    @Autowired RegistrationService regs;

    private String[] register(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"password123\",\"displayName\":\"" + email + "\"}"))
                .andReturn().getResponse().getContentAsString();
        var j = om.readTree(body);
        return new String[]{"Bearer " + j.get("token").asText(), j.get("user").get("id").asText()};
    }

    @Test
    void organizerSeesStatsAndParticipantsOnly() throws Exception {
        String[] org = register("org@example.com"), other = register("notorg@example.com");
        Event e = new Event(Long.valueOf(org[1]));
        e.setTitle("ダッシュボード検証"); e.setTimeZone("Asia/Tokyo"); e.setCapacity(2);
        e.setStartAt(Instant.now().plus(10, ChronoUnit.DAYS)); e.setEndAt(Instant.now().plus(11, ChronoUnit.DAYS));
        e = events.save(e);
        Long[] uids = new Long[4];
        for (int i = 0; i < 4; i++) {
            uids[i] = users.save(new User("p" + i + "@example.com", "x", "参加者" + i, null)).getId();
            regs.register(e.getId(), uids[i]);   // 2人確定・2人キャンセル待ち
        }
        regs.cancel(e.getId(), uids[3]);            // 待ちの1人がキャンセル

        mvc.perform(get("/api/dashboard").header("Authorization", org[0]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents").value(1))
                .andExpect(jsonPath("$.totalConfirmed").value(2))
                .andExpect(jsonPath("$.totalWaitlisted").value(1))
                .andExpect(jsonPath("$.averageFillRate").value(100.0));
        mvc.perform(get("/api/dashboard/events/" + e.getId()).header("Authorization", org[0]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.canceled").value(1))
                .andExpect(jsonPath("$.participants.length()").value(3))
                .andExpect(jsonPath("$.participants[0].email").value("p0@example.com"))
                .andExpect(jsonPath("$.trend[0].registrations").value(4));
        mvc.perform(get("/api/dashboard/events/" + e.getId()).header("Authorization", other[0])).andExpect(status().isForbidden());
        mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/dashboard").header("Authorization", other[0])).andExpect(jsonPath("$.totalEvents").value(0));
    }
}
