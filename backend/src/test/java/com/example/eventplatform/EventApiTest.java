package com.example.eventplatform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EventApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    private String token(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"password123\",\"displayName\":\"U\"}"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + om.readTree(body).get("token").asText();
    }

    private static final String EVENT = "{\"title\":\"Spring勉強会\",\"startAt\":\"2026-12-01T19:00:00+09:00\","
            + "\"endAt\":\"2026-12-01T21:00:00+09:00\",\"timeZone\":\"Asia/Tokyo\",\"capacity\":10,\"tags\":[\"Java\",\"spring\"]}";

    @Test
    void crudAndOwnership() throws Exception {
        String owner = token("owner@example.com"), other = token("other@example.com");
        String res = mvc.perform(post("/api/events").header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON).content(EVENT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startAt").value("2026-12-01T10:00:00Z"))
                .andExpect(jsonPath("$.startLocal").value("2026-12-01T19:00:00+09:00"))
                .andReturn().getResponse().getContentAsString();
        long id = om.readTree(res).get("id").asLong();

        mvc.perform(get("/api/events/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.tags.length()").value(2));
        mvc.perform(put("/api/events/" + id).header("Authorization", other)
                .contentType(MediaType.APPLICATION_JSON).content(EVENT)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/events/" + id).header("Authorization", other)).andExpect(status().isForbidden());
        mvc.perform(put("/api/events/" + id).header("Authorization", owner)
                .contentType(MediaType.APPLICATION_JSON).content(EVENT.replace("Spring勉強会", "更新後")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("更新後"));
        mvc.perform(delete("/api/events/" + id).header("Authorization", owner)).andExpect(status().isNoContent());
        mvc.perform(get("/api/events/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void validation() throws Exception {
        String t = token("v@example.com");
        mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(EVENT)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/events").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                .content(EVENT.replace("2026-12-01T21:00:00+09:00", "2026-12-01T18:00:00+09:00"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/events").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                .content(EVENT.replace("Asia/Tokyo", "Mars/Base"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/categories")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4));
    }
}
