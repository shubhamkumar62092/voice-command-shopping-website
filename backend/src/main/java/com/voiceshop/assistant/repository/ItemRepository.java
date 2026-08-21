package com.voiceshop.assistant.repository;

import com.voiceshop.assistant.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    Optional<Item> findFirstByNameIgnoreCaseAndPurchasedFalse(String name);

    List<Item> findByPurchasedFalse();

    List<Item> findByNameContainingIgnoreCase(String name);

    List<Item> findByCategoryIgnoreCase(String category);
}
