package com.example.shop.service;

import com.openai.client.OpenAIClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class AiService {
    private OpenAIClient openAIClient;

    public AiService(OpenAIClient openAIClient) {
        this.openAIClient = openAIClient;
    }

    public String ask(String prompt) {
        ResponseCreateParams params = ResponseCreateParams.builder()
                .model("gpt-4o-mini")
                .input(prompt)
                .build();

        Response response = openAIClient.responses().create(params);
        return response.output().stream()
                .flatMap(item -> item.message().stream())
                .flatMap(msg -> msg.content().stream())
                .flatMap(content -> content.outputText().stream())
                .map(outputText -> outputText.text())
                .collect(Collectors.joining());
    }
}
