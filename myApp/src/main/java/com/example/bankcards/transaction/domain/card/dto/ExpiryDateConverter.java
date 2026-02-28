package com.example.bankcards.transaction.domain.card.dto;

import com.example.bankcards.transaction.domain.card.params.ExpiryDate;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.YearMonth;

@Converter
public class ExpiryDateConverter implements AttributeConverter<ExpiryDate, YearMonth> {
    @Override
    public YearMonth convertToDatabaseColumn(ExpiryDate attribute) {
        return attribute != null ? attribute.value() : null;
    }

    @Override
    public ExpiryDate convertToEntityAttribute(YearMonth dbData) {
        return dbData != null ? new ExpiryDate(dbData) : null;
    }
}