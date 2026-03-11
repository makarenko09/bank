package com.example.bankcards.transaction.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;

@Converter
public class SettlementAccountConverter implements AttributeConverter<SettlementAccount, BigDecimal> {

    @Override
    public BigDecimal convertToDatabaseColumn(SettlementAccount attribute) {
        return attribute != null ? attribute.amount() : null;
    }

    @Override
    public SettlementAccount convertToEntityAttribute(BigDecimal dbData) {
        return dbData != null ? new SettlementAccount(dbData) : null;
    }
}