package com.opsflow.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.request.Category;
import com.opsflow.request.Priority;
import com.opsflow.request.Request;
import com.opsflow.request.RequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** The model-backed provider against a stand-in HTTP server: what it sends, and how it copes with bad answers. */
class OpenAiCompatibleProviderTest {

    private MockRestServiceServer server;
    private OpenAiCompatibleProvider provider;
    private Evidence evidence;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        ActionPolicy policy = new ActionPolicy();
        provider = new OpenAiCompatibleProvider(builder, "http://model.test/v1", "test-key", "test-model",
                new ObjectMapper(), policy, new RuleBasedAIProvider(policy));

        Request request = Request.builder()
                .externalReference("REQ-1").title("Charged twice").description("Card was charged twice for one order")
                .category(Category.PAYMENT).priority(Priority.HIGH).status(RequestStatus.INVESTIGATING).createdBy("agent")
                .build();
        evidence = new Evidence(request, 3, 2, 1, 2, List.of());
    }

    private static String modelAnswers(String contentJson) throws Exception {
        return new ObjectMapper().writeValueAsString(java.util.Map.of("choices",
                List.of(java.util.Map.of("message", java.util.Map.of("role", "assistant", "content", contentJson)))));
    }

    @Test
    void itSendsTheEvidenceAndTheAllowedActionsAndReadsTheAnswer() throws Exception {
        server.expect(requestTo("http://model.test/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("REPLAY_PAYMENT_EVENT")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Charged twice")))
                .andRespond(withSuccess(modelAnswers(
                        "{\"summary\":\"Duplicate charge\",\"likelyCause\":\"Retry without idempotency\","
                                + "\"recommendedAction\":\"REPLAY_PAYMENT_EVENT\",\"confidence\":0.8}"), MediaType.APPLICATION_JSON));

        AIProvider.Recommendation recommendation = provider.recommend(evidence);

        assertEquals("REPLAY_PAYMENT_EVENT", recommendation.getRecommendedAction());
        assertEquals("Retry without idempotency", recommendation.getLikelyCause());
        assertEquals(0.8, recommendation.getConfidence(), 1e-9);
        server.verify();
    }

    @Test
    void aConfidenceOutsideZeroToOneIsClamped() throws Exception {
        server.expect(requestTo("http://model.test/v1/chat/completions")).andRespond(withSuccess(modelAnswers(
                "{\"summary\":\"s\",\"likelyCause\":\"c\",\"recommendedAction\":\"ESCALATE_TO_ON_CALL\",\"confidence\":7}"),
                MediaType.APPLICATION_JSON));

        assertEquals(1.0, provider.recommend(evidence).getConfidence(), 1e-9);
    }

    @Test
    void whenTheModelIsDownTheRulesAnswerInItsPlace() {
        server.expect(requestTo("http://model.test/v1/chat/completions")).andRespond(withServerError());

        AIProvider.Recommendation recommendation = provider.recommend(evidence);

        assertEquals("REPLAY_PAYMENT_EVENT", recommendation.getRecommendedAction(), "the usual action for a payment request");
    }

    @Test
    void anAnswerThatIsNotJsonAlsoFallsBackToTheRules() throws Exception {
        server.expect(requestTo("http://model.test/v1/chat/completions"))
                .andRespond(withSuccess(modelAnswers("Sure! I think you should refund everyone."), MediaType.APPLICATION_JSON));

        assertTrue(new ActionPolicy().isKnown(provider.recommend(evidence).getRecommendedAction()));
    }
}
