// package com.example.bankcards.transaction.domain.card.dto;

// import java.util.UUID;

// import com.example.bankcards.transaction.domain.card.params.CardId;

// import jakarta.persistence.AttributeConverter;
// import jakarta.persistence.Converter;

// @Converter
// public class CardIdConverter implements AttributeConverter<CardId, UUID> {

// @Override
// public UUID convertToDatabaseColumn(CardId attribute) {

// return attribute != null ? attribute.value() : null;
// }

// @Override
// public CardId convertToEntityAttribute(UUID dbData) {
// return dbData != null ? new CardId(dbData) : null;
// }
// }