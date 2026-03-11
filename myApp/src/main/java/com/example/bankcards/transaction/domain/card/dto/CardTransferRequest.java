package com.example.bankcards.transaction.domain.card.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CardTransferRequest(
        @JsonProperty("from_card_id") UUID fromCardId,
        @JsonProperty("to_card_id") UUID toCardId,
        @JsonProperty("amount") BigDecimal amount) {

    public UUID getFromCardId() {
        return fromCardId;
    }

    public UUID getToCardId() {
        return toCardId;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
