package com.opsflow.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.outbox.OutboxEvent;
import com.opsflow.outbox.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The HTTP contract, through the real security filters, against the seeded in-memory database. */
@SpringBootTest
@AutoConfigureMockMvc
class RequestApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OutboxRepository outboxRepository;

    private String tokenFor(String username) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + username + "123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + data(result).get("accessToken").asText();
    }

    private JsonNode data(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    private JsonNode createRequest(String token) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/requests")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Charged but order still pending\",\"description\":\"Customer paid, order did not update.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andReturn();
        return data(result);
    }

    private String transition(String target, Long expectedVersion) {
        return "{\"targetStatus\":\"" + target + "\",\"comment\":\"test\""
                + (expectedVersion == null ? "" : ",\"expectedVersion\":" + expectedVersion) + "}";
    }

    @Test
    void withoutATokenTheApiIsClosed() throws Exception {
        mvc.perform(get("/api/v1/requests")).andExpect(status().isForbidden());
    }

    @Test
    void aWrongPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"agent\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void creatingARequestAlsoWritesItsEventToTheOutbox() throws Exception {
        JsonNode created = createRequest(tokenFor("agent"));

        List<OutboxEvent> events = outboxRepository.findAll().stream()
                .filter(e -> e.getAggregateType().equals("REQUEST") && e.getAggregateId().equals(created.get("id").asText()))
                .toList();
        assertEquals(1, events.size());
        assertEquals("REQUEST_CREATED", events.get(0).getEventType());
        assertNotNull(events.get(0).getEventId());
        assertEquals(OutboxEvent.OutboxStatus.PENDING, events.get(0).getStatus(),
                "no broker in this test, so the event must wait and not be marked as sent");
    }

    @Test
    void theListCanBeSearchedWithoutCaringAboutCase() throws Exception {
        String agent = tokenFor("agent");
        JsonNode created = createRequest(agent);

        mvc.perform(get("/api/v1/requests").header("Authorization", agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isNotEmpty());
        mvc.perform(get("/api/v1/requests").param("query", "ORDER STILL pending").param("size", "100")
                        .header("Authorization", agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == " + created.get("id").asLong() + ")]").exists());
        mvc.perform(get("/api/v1/requests").param("query", "no request says this").header("Authorization", agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void aViewerCanReadButCannotChangeARequest() throws Exception {
        JsonNode created = createRequest(tokenFor("agent"));
        String viewer = tokenFor("viewer");

        mvc.perform(get("/api/v1/requests/" + created.get("id").asLong()).header("Authorization", viewer))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/requests/" + created.get("id").asLong() + "/transition")
                        .header("Authorization", viewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transition("CLASSIFIED", null)))
                .andExpect(status().isForbidden());
    }

    @Test
    void aTransitionTheWorkflowDoesNotAllowIsRefused() throws Exception {
        String agent = tokenFor("agent");
        JsonNode created = createRequest(agent);

        mvc.perform(post("/api/v1/requests/" + created.get("id").asLong() + "/transition")
                        .header("Authorization", agent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transition("CLOSED", null)))
                .andExpect(status().isConflict());
    }

    @Test
    void theSecondOfTwoEditsFromTheSameStaleCopyGetsAConflict() throws Exception {
        String agent = tokenFor("agent");
        JsonNode created = createRequest(agent);
        long id = created.get("id").asLong();
        long versionBothPeopleRead = created.get("version").asLong();

        mvc.perform(post("/api/v1/requests/" + id + "/transition")
                        .header("Authorization", agent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transition("CLASSIFIED", versionBothPeopleRead)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLASSIFIED"));

        mvc.perform(post("/api/v1/requests/" + id + "/transition")
                        .header("Authorization", agent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transition("ASSIGNED", versionBothPeopleRead)))
                .andExpect(status().isConflict());
    }

    @Test
    void someoneWhoRegistersCannotMakeThemselvesAnAdmin() throws Exception {
        MvcResult registered = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"mallory\",\"password\":\"mallory123\",\"fullName\":\"Mallory\","
                                + "\"email\":\"mallory@example.com\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("VIEWER", data(registered).get("user").get("role").asText());

        mvc.perform(post("/api/v1/requests/1/transition")
                        .header("Authorization", "Bearer " + data(registered).get("accessToken").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transition("CLASSIFIED", null)))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyAManagerCanApprove() throws Exception {
        mvc.perform(post("/api/v1/approvals/999999/approve")
                        .header("Authorization", tokenFor("agent"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
