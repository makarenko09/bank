package com.example.bankcards.transaction.domain.card.dto;

import java.util.Set;
import java.util.UUID;

import com.example.bankcards.transaction.domain.card.Card;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ClientAccountWithCard(
        @JsonProperty("client_id") UUID userId,
        @JsonProperty("client_name") String ownerName,
        @JsonProperty("client_cards") Set<Card> cards) {
    // Jackson автоматически использует canonical constructor record
    // @JsonCreator можно не указывать явно

    public UUID getUserId() {
        return userId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public Set<Card> getCards() {
        return cards;
    }
}
