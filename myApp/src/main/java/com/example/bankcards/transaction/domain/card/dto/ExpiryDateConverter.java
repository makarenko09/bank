package com.example.bankcards.transaction.domain.card.dto;

import com.example.bankcards.transaction.domain.card.params.ExpiryDate;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.sql.Date;
import java.time.YearMonth;

@Converter
public class ExpiryDateConverter implements AttributeConverter<ExpiryDate, Date> {
    @Override
    public Date convertToDatabaseColumn(ExpiryDate attribute) {
        if (attribute == null) {
            return null;
        }
        YearMonth yearMonth = attribute.value();
        return yearMonth != null ? Date.valueOf(yearMonth.atDay(1)) : null;
    }

    @Override
    public ExpiryDate convertToEntityAttribute(Date dbData) {
        if (dbData == null) {
            return null;
        }
        YearMonth yearMonth = YearMonth.from(dbData.toLocalDate());
        return new ExpiryDate(yearMonth);
    }
}