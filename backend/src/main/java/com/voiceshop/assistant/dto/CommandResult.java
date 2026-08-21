package com.voiceshop.assistant.dto;

import com.voiceshop.assistant.model.Item;

import java.util.List;

public class CommandResult {

    private String intent;
    private boolean success;
    private String message;
    private Item affectedItem;
    private List<Item> searchResults;
    private List<String> substituteSuggestions;

    public static CommandResult ok(String intent, String message) {
        CommandResult r = new CommandResult();
        r.intent = intent;
        r.success = true;
        r.message = message;
        return r;
    }

    public static CommandResult fail(String intent, String message) {
        CommandResult r = new CommandResult();
        r.intent = intent;
        r.success = false;
        r.message = message;
        return r;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Item getAffectedItem() {
        return affectedItem;
    }

    public void setAffectedItem(Item affectedItem) {
        this.affectedItem = affectedItem;
    }

    public List<Item> getSearchResults() {
        return searchResults;
    }

    public void setSearchResults(List<Item> searchResults) {
        this.searchResults = searchResults;
    }

    public List<String> getSubstituteSuggestions() {
        return substituteSuggestions;
    }

    public void setSubstituteSuggestions(List<String> substituteSuggestions) {
        this.substituteSuggestions = substituteSuggestions;
    }
}
