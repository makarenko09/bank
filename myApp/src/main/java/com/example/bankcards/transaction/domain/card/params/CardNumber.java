package com.example.bankcards.transaction.domain.card.params;

import java.util.concurrent.ThreadLocalRandom;

import com.example.bankcards.shared.error.domain.Assert;

public record CardNumber(String value) {
    public CardNumber {
        Assert.field("CardNumber", value).notNull();
        Assert.field("CardNumber", value).minLength(15);
        Assert.field("CardNumber", value).maxLength(17);

    }

    public static CardNumber generate() {
        long number = ThreadLocalRandom.current().nextLong(1_000_000_000_000_000L, 10_000_000_000_000_000L);
        return new CardNumber(String.format("%016d", number));
    }

    public String masked() {
        return "**** **** **** " + value.substring(12);
    }

}