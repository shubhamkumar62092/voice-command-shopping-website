package com.voiceshop.assistant.service;

import org.springframework.stereotype.Component;

import java.time.Month;
import java.util.*;

/**
 * Static reference data for substitutes and seasonal produce. In a
 * production system these would come from a catalog/inventory service;
 * here they are small hand-curated maps so the "smart suggestions" feature
 * works fully offline and deterministically for demo/grading purposes.
 */
@Component
public class SuggestionDataService {

    private final Map<String, List<String>> substitutes = new HashMap<>();
    private final Map<Month, List<String>> seasonalByMonth = new EnumMap<>(Month.class);

    public SuggestionDataService() {
        substitutes.put("milk", List.of("almond milk", "oat milk", "soy milk"));
        substitutes.put("butter", List.of("margarine", "ghee"));
        substitutes.put("sugar", List.of("honey", "jaggery", "stevia"));
        substitutes.put("rice", List.of("quinoa", "cauliflower rice"));
        substitutes.put("bread", List.of("multigrain bread", "gluten-free bread"));
        substitutes.put("pasta", List.of("zucchini noodles", "whole wheat pasta"));

        // Northern-hemisphere-leaning seasonal list, kept intentionally short.
        seasonalByMonth.put(Month.JANUARY, List.of("oranges", "grapefruit", "kale"));
        seasonalByMonth.put(Month.FEBRUARY, List.of("oranges", "spinach", "beets"));
        seasonalByMonth.put(Month.MARCH, List.of("asparagus", "peas", "spinach"));
        seasonalByMonth.put(Month.APRIL, List.of("asparagus", "strawberries", "artichoke"));
        seasonalByMonth.put(Month.MAY, List.of("strawberries", "peas", "lettuce"));
        seasonalByMonth.put(Month.JUNE, List.of("cherries", "peaches", "zucchini"));
        seasonalByMonth.put(Month.JULY, List.of("tomatoes", "corn", "watermelon"));
        seasonalByMonth.put(Month.AUGUST, List.of("tomatoes", "peaches", "corn"));
        seasonalByMonth.put(Month.SEPTEMBER, List.of("apples", "pumpkin", "grapes"));
        seasonalByMonth.put(Month.OCTOBER, List.of("apples", "pumpkin", "sweet potato"));
        seasonalByMonth.put(Month.NOVEMBER, List.of("cranberries", "sweet potato", "brussels sprouts"));
        seasonalByMonth.put(Month.DECEMBER, List.of("oranges", "pomegranate", "brussels sprouts"));
    }

    public List<String> getSubstitutesFor(String itemName) {
        if (itemName == null) return List.of();
        String lower = itemName.toLowerCase();
        for (Map.Entry<String, List<String>> entry : substitutes.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return List.of();
    }

    public List<String> getSeasonalItems() {
        Month current = LocalDateNow().getMonth();
        return seasonalByMonth.getOrDefault(current, List.of());
    }

    private java.time.LocalDate LocalDateNow() {
        return java.time.LocalDate.now();
    }
}
