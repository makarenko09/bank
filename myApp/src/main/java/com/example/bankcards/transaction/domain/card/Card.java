package com.example.bankcards.transaction.domain.card;

import java.math.BigDecimal;
import java.util.UUID;
import com.example.bankcards.shared.error.domain.Assert;
import com.example.bankcards.transaction.domain.card.params.CardId;
import com.example.bankcards.transaction.domain.card.params.CardNumber;
import com.example.bankcards.transaction.domain.card.params.CardStatus;
import com.example.bankcards.transaction.domain.card.params.ExpiryDate;
import com.example.bankcards.transaction.domain.card.params.Money;

public class Card {
    private final CardId id;
    private final CardNumber number;
    // private final String ownerName;
    private final ExpiryDate expiryDate;

    private CardStatus status;
    private Money balance;

    // private final UUID userId;

    public Card(CardId id, CardNumber number, /* String ownerName, */ExpiryDate expiryDate, UUID userId) {
        this.id = id;
        this.number = number;
        // this.ownerName = ownerName;
        this.expiryDate = expiryDate;
        // this.userId = userId;
        this.status = CardStatus.BLOCKED;
        this.balance = new Money(BigDecimal.ZERO);
    }

    public void block() {
        this.status = CardStatus.BLOCKED;
    }

    public void activate() {
        this.status = CardStatus.ACTIVE;
    }

    @SuppressWarnings("unlikely-arg-type")
    public void cardValidatetior() {
        Assert.field("CardStatus is block", status.name()).equals(CardStatus.BLOCKED.name());
        Assert.field("CardStatus is expired", status.name()).equals(CardStatus.EXPIRED.name());

    }

    public CardNumber number() {
        return number;
    }

    public Money balance() {
        return balance;
    }

    public CardStatus status() {
        return status;
    }

    public String maskedNumber() {
        return number.masked();
    }

    public CardId getId() {
        return id;
    }

    public ExpiryDate getExpiryDate() {
        return expiryDate;
    }
}
