package com.example.bankcards.transaction.domain;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.example.bankcards.transaction.domain.card.Card;
import com.example.bankcards.transaction.domain.card.dto.MoneyConverter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
@JsonIgnoreProperties(value = { "cards" })
public class ClientAccount {

    @OneToMany(mappedBy = "account", orphanRemoval = true)
    @JsonManagedReference
    private final Set<Card> cards = new HashSet<>();

    @Id
    @GeneratedValue
    private final UUID userId;

    private final String ownerName;

    @Column(name = "bill", columnDefinition = "NUMERIC")
    @Convert(converter = MoneyConverter.class)
    private final SettlementAccount settlementAccount;

    public SettlementAccount getSettlementAccount() {
        return settlementAccount;
    }

    public ClientAccount(UUID userId, String ownerName) {
        this.userId = userId;
        this.ownerName = ownerName;
        this.settlementAccount = new SettlementAccount(BigDecimal.ZERO);
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