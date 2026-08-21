package com.voiceshop.assistant.controller;

import com.voiceshop.assistant.dto.CommandResult;
import com.voiceshop.assistant.service.ShoppingListService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/voice-command")
public class VoiceCommandController {

    private final ShoppingListService shoppingListService;

    public VoiceCommandController(ShoppingListService shoppingListService) {
        this.shoppingListService = shoppingListService;
    }

    public static class VoiceCommandRequest {
        @NotBlank
        public String transcript;
    }

    @PostMapping
    public ResponseEntity<CommandResult> handle(@RequestBody VoiceCommandRequest request) {
        if (request == null || request.transcript == null || request.transcript.isBlank()) {
            return ResponseEntity.badRequest().body(CommandResult.fail("UNKNOWN", "Empty transcript."));
        }
        CommandResult result = shoppingListService.handleTranscript(request.transcript);
        return ResponseEntity.ok(result);
    }
}
