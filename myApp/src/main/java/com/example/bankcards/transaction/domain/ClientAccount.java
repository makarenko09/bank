package com.example.bankcards.transaction.domain;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.example.bankcards.shared.collection.domain.BankCollections;
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
        // this.cards = buildCards();
    }

    public void addCard(Card card) {
        Assert.notNull("addCard on ClientAccount", card);
        this.cards.add(card);
    }

    public Set<Card> getCards() {
        return cards;
    }

    public static ClientAccount ClientAccountBuilder(String ownerName) {
        return new ClientAccount(UUID.randomUUID(), ownerName);
    }

    // private static Cards buildCards() {

    // return Cards.EMPTY;
    // }

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