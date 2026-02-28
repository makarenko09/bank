package com.example.bankcards.transaction.domain.card.dto;

import com.example.bankcards.transaction.domain.card.params.CardNumber;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class CardNumberConverter implements AttributeConverter<CardNumber, String> {

    @Override
    public String convertToDatabaseColumn(CardNumber attribute) {
        return attribute != null ? attribute.value() : null;
    }

    @Override
    public CardNumber convertToEntityAttribute(String dbData) {
        return dbData != null ? new CardNumber(dbData) : null;
    }
}