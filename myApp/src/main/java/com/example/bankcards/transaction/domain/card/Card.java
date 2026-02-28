// 
package com.example.bankcards.transaction.domain.card;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.bankcards.shared.error.domain.Assert;
import com.example.bankcards.transaction.domain.card.dto.CardNumberConverter;
import com.example.bankcards.transaction.domain.card.dto.ExpiryDateConverter;
import com.example.bankcards.transaction.domain.card.dto.MoneyConverter;
import com.example.bankcards.transaction.domain.card.params.CardId;
import com.example.bankcards.transaction.domain.card.params.CardNumber;
import com.example.bankcards.transaction.domain.card.params.CardStatus;
import com.example.bankcards.transaction.domain.card.params.ExpiryDate;
import com.example.bankcards.transaction.domain.card.params.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cards")
public class Card {

    @Id
    private final UUID id;
    // @ManyToOne(fetch = FetchType.EAGER)
    // @JoinColumn(name = "clientaccount_id")
    // @JsonBackReference
    // private ClientAccount account;

    @Column(nullable = false)
    @Convert(converter = CardNumberConverter.class)
    private final CardNumber number;

    public UUID getOwnerId() {
        return ownerId;
    }

    @Column(nullable = false, name = "expiry_end", columnDefinition = "DATE")
    @Convert(converter = ExpiryDateConverter.class)
    private final ExpiryDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private CardStatus status;

    @Column(name = "balance", columnDefinition = "NUMERIC")
    @Convert(converter = MoneyConverter.class)
    private Money balance;

    public Money getBalance() {
        return balance;
    }

    public CardNumber getNumber() {
        return number;
    }

    @Column(name = "user_id", nullable = false)
    private UUID ownerId;

    public Card(
            UUID id,
            CardNumber number,
            ExpiryDate expiryDate,
            UUID userId) {
        this.id = id;
        this.number = number;
        this.expiryDate = expiryDate;
        this.status = CardStatus.BLOCKED;
        this.balance = new Money(BigDecimal.ZERO);
        this.ownerId = userId;
    }

    public static Card CardBuilder(UUID ownerId) {
        return new Card(CardId.generate(), CardNumber.generate(), ExpiryDate.EXPIRYBEFORE, ownerId);
    }

    public void block() {
        this.status = CardStatus.BLOCKED;
    }

    public Card activate() {
        this.status = CardStatus.ACTIVE;
        return this;
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

    public UUID getId() {
        return id;
    }

    public ExpiryDate getExpiryDate() {
        return expiryDate;
    }
}
