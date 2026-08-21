package com.voiceshop.assistant.service;

import com.voiceshop.assistant.dto.CommandResult;
import com.voiceshop.assistant.dto.ParsedCommand;
import com.voiceshop.assistant.model.Item;
import com.voiceshop.assistant.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ShoppingListService {

    private final ItemRepository itemRepository;
    private final CommandParser commandParser;
    private final CategoryClassifier categoryClassifier;
    private final SuggestionDataService suggestionData;

    public ShoppingListService(ItemRepository itemRepository,
                                CommandParser commandParser,
                                CategoryClassifier categoryClassifier,
                                SuggestionDataService suggestionData) {
        this.itemRepository = itemRepository;
        this.commandParser = commandParser;
        this.categoryClassifier = categoryClassifier;
        this.suggestionData = suggestionData;
    }

    public CommandResult handleTranscript(String transcript) {
        ParsedCommand cmd = commandParser.parse(transcript);

        switch (cmd.getIntent()) {
            case ADD:
                return handleAdd(cmd);
            case REMOVE:
                return handleRemove(cmd);
            case SEARCH:
                return handleSearch(cmd);
            case CLEAR:
                return handleClear();
            default:
                return CommandResult.fail("UNKNOWN",
                        "Sorry, I didn't catch a clear command in \"" + transcript + "\".");
        }
    }

    private CommandResult handleAdd(ParsedCommand cmd) {
        if (cmd.getItemName() == null || cmd.getItemName().isBlank()) {
            return CommandResult.fail("ADD", "I didn't catch what item to add.");
        }

        Optional<Item> existing = itemRepository.findFirstByNameIgnoreCaseAndPurchasedFalse(cmd.getItemName());
        Item item;
        if (existing.isPresent()) {
            item = existing.get();
            item.setQuantity(item.getQuantity() + cmd.getQuantity());
            item.setTimesAdded(item.getTimesAdded() + 1);
        } else {
            item = new Item();
            item.setName(cmd.getItemName());
            item.setQuantity(cmd.getQuantity());
            item.setUnit(cmd.getUnit());
            item.setCategory(categoryClassifier.classify(cmd.getItemName()));
        }
        item = itemRepository.save(item);

        CommandResult result = CommandResult.ok("ADD",
                "Added " + item.getQuantity() + (item.getUnit() != null ? " " + item.getUnit() : "")
                        + " " + item.getName() + " to your list.");
        result.setAffectedItem(item);

        List<String> subs = suggestionData.getSubstitutesFor(item.getName());
        if (!subs.isEmpty()) {
            result.setSubstituteSuggestions(subs);
        }
        return result;
    }

    private CommandResult handleRemove(ParsedCommand cmd) {
        if (cmd.getItemName() == null || cmd.getItemName().isBlank()) {
            return CommandResult.fail("REMOVE", "I didn't catch what item to remove.");
        }
        Optional<Item> existing = itemRepository.findFirstByNameIgnoreCaseAndPurchasedFalse(cmd.getItemName());
        if (existing.isEmpty()) {
            return CommandResult.fail("REMOVE", "\"" + cmd.getItemName() + "\" isn't on your list.");
        }
        itemRepository.delete(existing.get());
        return CommandResult.ok("REMOVE", "Removed " + existing.get().getName() + " from your list.");
    }

    private CommandResult handleSearch(ParsedCommand cmd) {
        List<Item> results = cmd.getItemName() == null || cmd.getItemName().isBlank()
                ? itemRepository.findAll()
                : itemRepository.findByNameContainingIgnoreCase(cmd.getItemName());

        if (cmd.getMaxPrice() != null) {
            results = results.stream()
                    .filter(i -> i.getPrice() != null && i.getPrice() <= cmd.getMaxPrice())
                    .toList();
        }

        CommandResult result = CommandResult.ok("SEARCH",
                results.isEmpty() ? "No matching items found." : "Found " + results.size() + " matching item(s).");
        result.setSearchResults(results);
        return result;
    }

    private CommandResult handleClear() {
        long count = itemRepository.count();
        itemRepository.deleteAll();
        return CommandResult.ok("CLEAR", "Cleared " + count + " item(s) from your list.");
    }

    public List<Item> getActiveList() {
        return itemRepository.findByPurchasedFalse();
    }
}
