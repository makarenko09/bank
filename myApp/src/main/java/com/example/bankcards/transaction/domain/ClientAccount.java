package com.example.bankcards.transaction.domain;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.example.bankcards.transaction.domain.card.Card;

public class ClientAccount {
    private final Set<Card> cards = new HashSet<>();

    private final UUID userId;
    private final String ownerName;
    private final SettlementAccount settlementAccount;

    public SettlementAccount getSettlementAccount() {
        return settlementAccount;
    }

    public ClientAccount(UUID userId, String ownerName) {
        this.userId = userId;
        this.ownerName = ownerName;
        this.settlementAccount = new SettlementAccount(BigDecimal.ZERO);
    }

    public static ClientAccount ClientAccountBuilder(String ownerName) {
        return new ClientAccount(UUID.randomUUID(), ownerName);
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