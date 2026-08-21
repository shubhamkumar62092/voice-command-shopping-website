package com.voiceshop.assistant.controller;

import com.voiceshop.assistant.service.SmartSuggestionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/suggestions")
public class SuggestionController {

    private final SmartSuggestionService smartSuggestionService;

    public SuggestionController(SmartSuggestionService smartSuggestionService) {
        this.smartSuggestionService = smartSuggestionService;
    }

    @GetMapping
    public Map<String, Object> getSuggestions() {
        return smartSuggestionService.buildSuggestions();
    }
}
