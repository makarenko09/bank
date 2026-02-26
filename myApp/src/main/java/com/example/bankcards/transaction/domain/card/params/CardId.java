package com.example.bankcards.transaction.domain.card.params;

import java.util.UUID;

public record CardId(UUID value) {

    public static CardId generate() {
        return new CardId(UUID.randomUUID());
    }

    public static CardId fromString(String str) {
        return new CardId(UUID.fromString(str));
    }
}