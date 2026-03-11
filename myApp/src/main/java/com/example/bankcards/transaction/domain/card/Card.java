package com.example.bankcards.transaction.domain.card;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.bankcards.shared.error.domain.Assert;
import com.example.bankcards.transaction.domain.ClientAccount;
import com.example.bankcards.transaction.domain.card.dto.CardNumberConverter;
import com.example.bankcards.transaction.domain.card.dto.ExpiryDateConverter;
import com.example.bankcards.transaction.domain.card.dto.MoneyConverter;
import com.example.bankcards.transaction.domain.card.params.CardNumber;
import com.example.bankcards.transaction.domain.card.params.CardStatus;
import com.example.bankcards.transaction.domain.card.params.ExpiryDate;
import com.example.bankcards.transaction.domain.card.params.Money;
import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Card {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id")
    @JsonBackReference
    private ClientAccount account;

    @Column(nullable = false)
    @Convert(converter = CardNumberConverter.class)
    private CardNumber number;

    @Column(nullable = false, name = "expiry_end", columnDefinition = "DATE")
    @Convert(converter = ExpiryDateConverter.class)
    private ExpiryDate expiryDate;

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

    public Card(
            UUID userId) {

        this.number = CardNumber.generate();
        this.expiryDate = ExpiryDate.EXPIRYBEFORE;
        this.status = CardStatus.BLOCKED;
        this.balance = new Money(BigDecimal.ZERO);
    }

    public void block() {
        this.status = CardStatus.BLOCKED;
    }

    public Card() {
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

    public void setAccount(ClientAccount account) {
        this.account = account;
    }

    public void setStatus(CardStatus status) {
        this.status = status;
    }

    public void setBalance(Money balance) {
        this.balance = balance;
    }

    public ClientAccount getAccount() {
        return account;
    }
}
