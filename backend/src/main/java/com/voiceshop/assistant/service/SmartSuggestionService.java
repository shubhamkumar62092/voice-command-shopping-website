package com.voiceshop.assistant.service;

import com.voiceshop.assistant.model.Item;
import com.voiceshop.assistant.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SmartSuggestionService {

    private final ItemRepository itemRepository;
    private final SuggestionDataService suggestionData;

    public SmartSuggestionService(ItemRepository itemRepository, SuggestionDataService suggestionData) {
        this.itemRepository = itemRepository;
        this.suggestionData = suggestionData;
    }

    public Map<String, Object> buildSuggestions() {
        Map<String, Object> suggestions = new LinkedHashMap<>();

        Set<String> currentListLower = itemRepository.findByPurchasedFalse().stream()
                .map(i -> i.getName().toLowerCase())
                .collect(Collectors.toSet());

        // "Frequently bought" - items with a high historical add-count that
        // are not currently on the active list ("looks like you're running
        // low on X" style nudge).
        List<String> frequentlyBought = itemRepository.findAll().stream()
                .filter(i -> i.getTimesAdded() >= 2)
                .filter(i -> !currentListLower.contains(i.getName().toLowerCase()))
                .sorted(Comparator.comparingInt(Item::getTimesAdded).reversed())
                .map(Item::getName)
                .distinct()
                .limit(5)
                .toList();
        suggestions.put("frequentlyBought", frequentlyBought);

        // Seasonal items not already on the list.
        List<String> seasonal = suggestionData.getSeasonalItems().stream()
                .filter(name -> !currentListLower.contains(name.toLowerCase()))
                .toList();
        suggestions.put("seasonal", seasonal);

        return suggestions;
    }
}
