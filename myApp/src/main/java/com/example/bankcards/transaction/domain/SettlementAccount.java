package com.example.bankcards.transaction.domain;

import java.math.BigDecimal;

import com.example.bankcards.shared.error.domain.Assert;

class SettlementAccount {
    private final BigDecimal valueOnSettlementAccount;

    public SettlementAccount(BigDecimal valueOnSettlementAccount) {
        Assert.field("Account", valueOnSettlementAccount).strictlyPositive();
        this.valueOnSettlementAccount = valueOnSettlementAccount;
    }

    public static SettlementAccount SettlementAccountBuilder() {
        return new SettlementAccount(BigDecimal.ZERO);
    }

}
