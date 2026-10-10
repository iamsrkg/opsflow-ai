package com.opsflow.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.approval.ApprovalRepository;
import com.opsflow.approval.ApprovalRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A request from creation to resolution, over HTTP: the AI recommends, a manager decides. */
@SpringBootTest
@AutoConfigureMockMvc
class InvestigationFlowTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ApprovalRepository approvalRepository;

    private String tokenFor(String username) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + username + "123\"}"))
                .andExpect(status().isOk()).andReturn();
        return "Bearer " + data(result).get("accessToken").asText();
    }

    private JsonNode data(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    private long create(String token, String title, String description) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/requests").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("title", title, "description", description))))
                .andExpect(status().isCreated()).andReturn();
        return data(result).get("id").asLong();
    }

    private String statusOf(long id, String token) throws Exception {
        return data(mvc.perform(get("/api/v1/requests/" + id).header("Authorization", token)).andReturn()).get("status").asText();
    }

    @Test
    void aNewPaymentRequestIsInvestigatedThenWaitsForAManagerThenIsResolved() throws Exception {
        String agent = tokenFor("agent");
        long id = create(agent, "Customer charged but order still pending",
                "Payment was deducted from the card, the order status did not update.");

        MvcResult investigated = mvc.perform(post("/api/v1/requests/" + id + "/investigate").header("Authorization", agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendedAction").value("REPLAY_PAYMENT_EVENT"))
                .andExpect(jsonPath("$.data.requiresApproval").value(true))
                .andReturn();
        JsonNode investigation = data(investigated);
        assertTrue(investigation.get("relevantRunbookTitle").asText().contains("Payment"));
        assertTrue(investigation.get("evidenceJson").asText().contains("similarity"), "evidence quotes the real runbook match");
        assertEquals("PENDING_APPROVAL", statusOf(id, agent), "nothing has run yet: it waits for a manager");

        ApprovalRequest approval = approvalRepository.findAll().stream()
                .filter(a -> a.getRequestId().equals(id)).findFirst().orElseThrow();

        mvc.perform(post("/api/v1/approvals/" + approval.getId() + "/approve").header("Authorization", agent)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        assertEquals("PENDING_APPROVAL", statusOf(id, agent), "an agent can't approve, so nothing changed");

        mvc.perform(post("/api/v1/approvals/" + approval.getId() + "/approve").header("Authorization", tokenFor("manager"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"comment\":\"ok\"}"))
                .andExpect(status().isOk());
        assertEquals("RESOLVED", statusOf(id, agent));
    }

    @Test
    void aRequestAssignedByHandBeforeAnyoneClassifiedItIsStillClassifiedBeforeTheRecommendation() throws Exception {
        String agent = tokenFor("agent");
        long id = create(agent, "Customer charged but order still pending", "Payment was deducted, the order did not update.");
        mvc.perform(post("/api/v1/requests/" + id + "/assign").header("Authorization", agent)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"assignedTo\":\"agent\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/requests/" + id + "/investigate").header("Authorization", agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendedAction").value("REPLAY_PAYMENT_EVENT"));
    }

    @Test
    void aRequestNoRuleUnderstandsIsNotGivenAnActionThatNeedsApproval() throws Exception {
        String agent = tokenFor("agent");
        long id = create(agent, "Question about opening hours", "The customer wants to know when the office is open.");

        mvc.perform(post("/api/v1/requests/" + id + "/investigate").header("Authorization", agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendedAction").value("REQUEST_MORE_INFORMATION"))
                .andExpect(jsonPath("$.data.requiresApproval").value(false));
        assertEquals("INVESTIGATING", statusOf(id, agent));
    }
}
