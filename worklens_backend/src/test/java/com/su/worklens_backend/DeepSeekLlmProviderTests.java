package com.su.worklens_backend;

import com.su.worklens_backend.service.impl.DeepSeekLlmProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DeepSeekLlmProviderTests {

    @Test
    void generateTextPostsPromptToDeepSeekAndReturnsAssistantMessage() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        DeepSeekLlmProvider provider = new DeepSeekLlmProvider(
                restTemplate,
                "https://api.deepseek.com",
                "test-api-key",
                "deepseek-v4-flash"
        );

        server.expect(requestTo("https://api.deepseek.com/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "model": "deepseek-v4-flash",
                          "messages": [
                            {
                              "role": "user",
                              "content": "Summarize this fixed text."
                            }
                          ]
                        }
                        """, true))
                .andRespond(withSuccess("""
                        {
                          "choices": [
                            {
                              "message": {
                                "content": "This is the generated response."
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        String response = provider.generateText("Summarize this fixed text.");

        assertThat(response).isEqualTo("This is the generated response.");
        server.verify();
    }
}
