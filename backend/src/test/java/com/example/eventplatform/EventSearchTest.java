package com.example.eventplatform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:searchdb;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@org.junit.jupiter.api.TestInstance(org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS)
class EventSearchTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    String token;

    private void create(String title, String desc, String start, int cap, String tags, int categoryId) throws Exception {
        String end = start.replace("T10", "T12");
        mvc.perform(post("/api/events").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + title + "\",\"description\":\"" + desc + "\",\"startAt\":\"" + start
                        + "\",\"endAt\":\"" + end + "\",\"timeZone\":\"Asia/Tokyo\",\"capacity\":" + cap
                        + ",\"categoryId\":" + categoryId + ",\"tags\":" + tags + "}")).andExpect(status().isCreated());
    }

    @BeforeAll
    void setUp() throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"s@example.com\",\"password\":\"password123\",\"displayName\":\"S\"}"))
                .andReturn().getResponse().getContentAsString();
        token = "Bearer " + om.readTree(body).get("token").asText();
        create("Java入門", "100% 基礎", "2030-01-10T10:00:00Z", 1, "[\"java\",\"beginner\"]", 1);
        create("React勉強会", "フロント", "2030-02-10T10:00:00Z", 10, "[\"react\",\"beginner\"]", 2);
        create("Spring実践", "backend", "2030-03-10T10:00:00Z", 10, "[\"java\",\"spring\"]", 2);
    }

    @Test
    void keywordAndWildcardEscape() throws Exception {
        mvc.perform(get("/api/events").param("q", "react")).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/events").param("q", "%")).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/events").param("q", "nomatch")).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void tagsAreAnded_categoryAndOrder() throws Exception {
        mvc.perform(get("/api/events").param("tag", "java")).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/events").param("tag", "java", "beginner")).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Java入門"));
        mvc.perform(get("/api/events").param("categoryId", "2")).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].title").value("React勉強会"));
    }

    @Test
    void dateRangeAndAvailability() throws Exception {
        mvc.perform(get("/api/events").param("from", "2030-02-01T00:00:00Z").param("to", "2030-03-01T00:00:00Z"))
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].title").value("React勉強会"));
        // 定員1のJava入門に申込んで満席にすると、availableOnlyで除外される
        mvc.perform(get("/api/events").param("availableOnly", "true")).andExpect(jsonPath("$.totalElements").value(3));
        long id = om.readTree(mvc.perform(get("/api/events").param("q", "Java入門")).andReturn().getResponse().getContentAsString())
                .get("content").get(0).get("id").asLong();
        mvc.perform(post("/api/events/" + id + "/registrations").header("Authorization", token)).andExpect(status().isCreated());
        mvc.perform(get("/api/events").param("availableOnly", "true")).andExpect(jsonPath("$.totalElements").value(2));
    }
}
