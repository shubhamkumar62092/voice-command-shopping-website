package com.voiceshop.assistant.dto;

public class ParsedCommand {

    public enum Intent {
        ADD, REMOVE, SEARCH, CLEAR, UNKNOWN
    }

    private Intent intent = Intent.UNKNOWN;
    private String itemName;
    private int quantity = 1;
    private String unit;
    private Double maxPrice;
    private String rawTranscript;

    public Intent getIntent() {
        return intent;
    }

    public void setIntent(Intent intent) {
        this.intent = intent;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Double getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Double maxPrice) {
        this.maxPrice = maxPrice;
    }

    public String getRawTranscript() {
        return rawTranscript;
    }

    public void setRawTranscript(String rawTranscript) {
        this.rawTranscript = rawTranscript;
    }
}
