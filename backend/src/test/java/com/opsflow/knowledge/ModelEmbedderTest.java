package com.opsflow.knowledge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** The embedding-model client against a stand-in HTTP server. */
class ModelEmbedderTest {

    private MockRestServiceServer server;
    private ModelEmbedder embedder;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        embedder = new ModelEmbedder(builder, "http://model.test/v1", "", "test-embedder", 3, 0.3);
    }

    @Test
    void itAsksTheEmbeddingsEndpointAndReturnsAVectorOfLengthOne() {
        server.expect(requestTo("http://model.test/v1/embeddings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("test-embedder")))
                .andExpect(content().string(containsString("card charged twice")))
                .andRespond(withSuccess("{\"data\":[{\"embedding\":[3.0,0.0,4.0]}]}", MediaType.APPLICATION_JSON));

        float[] vector = embedder.embed("card charged twice");

        assertEquals(3, vector.length);
        assertEquals(0.6f, vector[0], 1e-6);
        assertEquals(0.8f, vector[2], 1e-6);
        assertEquals(1.0, EmbeddingService.cosine(vector, vector), 1e-6, "normalised, so cosine is a plain dot product");
        assertEquals("model:test-embedder", embedder.name());
        server.verify();
    }

    @Test
    void aVectorOfTheWrongSizeIsRefusedAndNotStored() {
        server.expect(requestTo("http://model.test/v1/embeddings"))
                .andRespond(withSuccess("{\"data\":[{\"embedding\":[1.0,2.0]}]}", MediaType.APPLICATION_JSON));

        IllegalStateException refused = assertThrows(IllegalStateException.class, () -> embedder.embed("anything"));

        assertTrue(refused.getMessage().contains("2 dimensions"), refused.getMessage());
    }

    @Test
    void aFailingModelServerIsAnErrorTheCallerCanHandle() {
        server.expect(requestTo("http://model.test/v1/embeddings")).andRespond(withServerError());

        assertThrows(RuntimeException.class, () -> embedder.embed("anything"));
    }
}
