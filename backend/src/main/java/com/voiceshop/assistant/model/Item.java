package com.voiceshop.assistant.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private int quantity = 1;

    private String unit;

    private String category;

    private Double price;

    private boolean purchased = false;

    // Tracks how many times this item name has ever been added, across the
    // list's lifetime. Used by SuggestionService to power "you usually buy
    // this" style recommendations.
    private int timesAdded = 1;

    private LocalDateTime addedAt = LocalDateTime.now();

    public Item() {
    }

    public Item(String name, int quantity, String unit, String category, Double price) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.category = category;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public boolean isPurchased() {
        return purchased;
    }

    public void setPurchased(boolean purchased) {
        this.purchased = purchased;
    }

    public int getTimesAdded() {
        return timesAdded;
    }

    public void setTimesAdded(int timesAdded) {
        this.timesAdded = timesAdded;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }
}
