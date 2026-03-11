package com.example.bankcards.transaction.domain;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.example.bankcards.transaction.domain.card.Card;
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
    private Set<Card> cards = new HashSet<>();

    @Id
    @GeneratedValue
    private UUID userId;

    @Column(unique = true, nullable = false)
    private String ownerName;

    @Column(name = "bill", columnDefinition = "NUMERIC")
    @Convert(converter = SettlementAccountConverter.class)
    private SettlementAccount settlementAccount;

    public SettlementAccount getSettlementAccount() {
        return settlementAccount;
    }

    public ClientAccount(String ownerName) {
        this.ownerName = ownerName;
        this.settlementAccount = new SettlementAccount(BigDecimal.ZERO);
    }

    public ClientAccount() {
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