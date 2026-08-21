package com.voiceshop.assistant.controller;

import com.voiceshop.assistant.model.Item;
import com.voiceshop.assistant.repository.ItemRepository;
import com.voiceshop.assistant.service.CategoryClassifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemRepository itemRepository;
    private final CategoryClassifier categoryClassifier;

    public ItemController(ItemRepository itemRepository, CategoryClassifier categoryClassifier) {
        this.itemRepository = itemRepository;
        this.categoryClassifier = categoryClassifier;
    }

    @GetMapping
    public List<Item> getAll() {
        return itemRepository.findByPurchasedFalse();
    }

    @PostMapping
    public ResponseEntity<Item> create(@RequestBody Item item) {
        if (item.getCategory() == null || item.getCategory().isBlank()) {
            item.setCategory(categoryClassifier.classify(item.getName()));
        }
        if (item.getQuantity() <= 0) {
            item.setQuantity(1);
        }
        return ResponseEntity.ok(itemRepository.save(item));
    }

    @PatchMapping("/{id}/purchased")
    public ResponseEntity<Item> togglePurchased(@PathVariable Long id) {
        return itemRepository.findById(id)
                .map(item -> {
                    item.setPurchased(!item.isPurchased());
                    return ResponseEntity.ok(itemRepository.save(item));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!itemRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        itemRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearAll() {
        itemRepository.deleteAll();
        return ResponseEntity.noContent().build();
    }
}
