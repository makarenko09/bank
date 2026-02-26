package com.example.bankcards.transaction.domain;

import java.util.Set;
import com.example.bankcards.shared.collection.domain.BankCollections;
import com.example.bankcards.transaction.domain.card.Card;

public record Cards(Set<Card> cards) {

    public static final Cards EMPTY = new Cards(null);

    public Cards(Set<Card> cards) {
        this.cards = BankCollections.immutable(cards);
    }

    public Set<Card> get() {
        return cards();
    }
}