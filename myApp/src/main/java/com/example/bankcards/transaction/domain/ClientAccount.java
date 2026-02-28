package com.example.bankcards.transaction.domain;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import com.example.bankcards.shared.error.domain.Assert;
import com.example.bankcards.transaction.domain.card.Card;

public class ClientAccount {
    private final Set<Card> cards = new HashSet<>();

    private final UUID userId;
    private final String ownerName;
    private final SettlementAccount settlementAccount;

    public ClientAccount(UUID userId, String ownerName) {
        this.userId = userId;
        this.ownerName = ownerName;
        this.settlementAccount = SettlementAccount.SettlementAccountBuilder();
    }

    public static ClientAccount ClientAccountBuilder(String ownerName) {
        return new ClientAccount(UUID.randomUUID(), ownerName);
    }

    public void addCard(Card card) {
        Assert.notNull("addCard on ClientAccount", card);
        this.cards.add(card);
    }

    public Set<Card> getCards() {
        return cards;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getOwnerName() {
        return ownerName;
    }

}