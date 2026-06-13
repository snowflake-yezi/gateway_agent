package com.gateway.agent.api;

import com.gateway.agent.config.GatewayProperties;
import com.gateway.agent.dto.openai.ChatCompletionRequest;
import com.gateway.agent.dto.openai.ChatCompletionResponse;
import com.gateway.agent.security.ApiKeyWebFilter;
import com.gateway.agent.service.ChatCompletionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(ChatCompletionsController.class)
@Import(ApiKeyWebFilter.class)
class ChatCompletionsControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private GatewayProperties gatewayProperties;

    @MockBean
    private ChatCompletionService chatCompletionService;

    @Test
    void createCompletionRejectsInvalidApiKey() {
        gatewayProperties.getAuth().setApiKey("test-key");

        webTestClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer wrong-key")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "model": "gateway-stub-chat",
                          "messages": [{"role": "user", "content": "hello"}]
                        }
                        """)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("invalid_api_key");
    }

    @Test
    void createCompletionReturnsStubResponse() {
        gatewayProperties.getAuth().setApiKey("test-key");
        ChatCompletionResponse response = new ChatCompletionResponse();
        response.setId("chatcmpl-test");
        response.setObject("chat.completion");
        response.setCreated(1L);
        response.setModel("gateway-stub-chat");
        response.setChoices(new ChatCompletionResponse.Choice[0]);

        when(chatCompletionService.createCompletion(any(ChatCompletionRequest.class))).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer test-key")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "model": "gateway-stub-chat",
                          "messages": [{"role": "user", "content": "hello"}]
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("chatcmpl-test")
                .jsonPath("$.object").isEqualTo("chat.completion")
                .jsonPath("$.model").isEqualTo("gateway-stub-chat");
    }
}
