package com.example.bankcards.transaction.domain;

import java.math.BigDecimal;

import com.example.bankcards.shared.error.domain.Assert;

public record SettlementAccount(BigDecimal amount) {

    public SettlementAccount {
        Assert.field("SettlementAccount", amount).positive();
    }
}
