package com.example.bankcards.transaction.domain;

import java.util.UUID;

public class ClientAccount {
    private static Cards cards;
    private final UUID userId;
    private final String ownerName;
    private final SettlementAccount settlementAccount;

    public ClientAccount(UUID userId, String ownerName) {
        this.userId = userId;
        this.ownerName = ownerName;
        this.settlementAccount = SettlementAccount.SettlementAccountBuilder();
        this.cards = buildCards();
    }

    public Cards getCards() {
        return cards;
    }

    public static ClientAccount ClientAccountBuilder(String ownerName) {
        return new ClientAccount(UUID.randomUUID(), ownerName);
    }

    private static Cards buildCards() {
        return Cards.EMPTY;
    }

    // public Client(List<Card> cards, UUID userId, String ownerName) {
    // this.cards = cards;
    // this.userId = userId;
    // this.ownerName = oCardswnerName;
    // this.settlementAccount = SettlementAccount.createAccount();
    // }

    public UUID getUserId() {
        return userId;
    }

    public String getOwnerName() {
        return ownerName;
    }

}