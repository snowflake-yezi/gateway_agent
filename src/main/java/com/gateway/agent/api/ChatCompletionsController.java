package com.gateway.agent.api;

import com.gateway.agent.dto.openai.ChatCompletionRequest;
import com.gateway.agent.dto.openai.ChatCompletionResponse;
import com.gateway.agent.service.ChatCompletionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/chat/completions")
public class ChatCompletionsController {

    private final ChatCompletionService chatCompletionService;

    public ChatCompletionsController(ChatCompletionService chatCompletionService) {
        this.chatCompletionService = chatCompletionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public Mono<ChatCompletionResponse> createCompletion(@Valid @RequestBody ChatCompletionRequest request) {
        return chatCompletionService.createCompletion(request);
    }
}
