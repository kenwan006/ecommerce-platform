package com.example.shop.controller;

import com.example.shop.dto.AiAskRequest;
import com.example.shop.service.AiService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

  private final AiService aiService;

  public AiController(AiService aiService) {
    this.aiService = aiService;
  }

  @PostMapping("/ask")
  public Map<String, String> generate(@RequestBody AiAskRequest prompt) {
    // Call your AI service here to generate a response based on the prompt
    String response =  aiService.ask(prompt.message());
    return Map.of("answer", response);
  }
}
