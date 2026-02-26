package com.example.bankcards.transaction.domain.card.params;

import java.time.YearMonth;

public record ExpiryDate(YearMonth value) {

    public static ExpiryDate ofYearsFromNow(int years) {
        return new ExpiryDate(YearMonth.now().plusYears(years));
    }

    public static ExpiryDate now() {
        return new ExpiryDate(YearMonth.now());
    }

    public boolean isExpired() {
        return value.isBefore(YearMonth.now());
    }

    public String toDisplayString() {
        return String.format("%02d/%02d", value.getMonthValue(), value.getYear() % 100);
    }
}