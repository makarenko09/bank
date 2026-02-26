package com.example.bankcards.transaction.domain.card.params;

import java.math.BigDecimal;

import com.example.bankcards.shared.error.domain.Assert;

public final record Money(BigDecimal amount) {

    public Money {
        Assert.field("Money", amount).positive();
    }

    // public BigDecimal getAmount() {
    // return amount;
    // }

}
