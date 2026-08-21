package com.voiceshop.assistant.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Very small keyword-based classifier that maps a raw item name to a grocery
 * aisle/category. This is intentionally simple (no external ML service) so
 * the whole project stays dependency-free and works offline - but it is
 * structured so swapping in a real NLP/ML classifier later is a one-file
 * change (just replace the body of classify()).
 */
@Component
public class CategoryClassifier {

    private final Map<String, String> keywordToCategory = new LinkedHashMap<>();

    public CategoryClassifier() {
        put("Dairy", "milk", "cheese", "yogurt", "butter", "cream", "curd", "paneer");
        put("Produce", "apple", "banana", "orange", "grape", "tomato", "onion", "potato",
                "carrot", "spinach", "lettuce", "cucumber", "mango", "berries", "avocado", "garlic", "lemon");
        put("Bakery", "bread", "bun", "bagel", "croissant", "cake", "muffin");
        put("Snacks", "chips", "cookies", "biscuit", "popcorn", "chocolate", "candy", "nuts");
        put("Beverages", "water", "juice", "soda", "coffee", "tea", "cola");
        put("Meat & Seafood", "chicken", "beef", "pork", "fish", "shrimp", "mutton", "egg", "eggs");
        put("Pantry", "rice", "pasta", "flour", "sugar", "salt", "oil", "cereal", "beans", "lentils", "spice");
        put("Household", "toothpaste", "soap", "shampoo", "detergent", "tissue", "paper towel", "cleaner");
        put("Frozen", "ice cream", "frozen", "pizza");
    }

    private void put(String category, String... keywords) {
        for (String k : keywords) {
            keywordToCategory.put(k, category);
        }
    }

    public String classify(String itemName) {
        if (itemName == null) return "General";
        String lower = itemName.toLowerCase();
        for (Map.Entry<String, String> entry : keywordToCategory.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "General";
    }
}
