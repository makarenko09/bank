package com.example.bankcards.transaction.infrastructure.primary;

import java.util.Collection;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.bankcards.transaction.domain.card.Card;
import com.example.bankcards.transaction.domain.card.params.CardStatus;
import com.example.bankcards.transaction.infrastructure.secondary.AdministratorClientManagment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/cards")
@Tag(name = "Card Management", description = "API для управления картами (ADMIN only)")
public class CardManagementController {

    private final AdministratorClientManagment administratorClientManagment;

    public CardManagementController(AdministratorClientManagment administratorClientManagment) {
        this.administratorClientManagment = administratorClientManagment;
    }

    @GetMapping
    @Operation(summary = "Получить все карты", description = "Возвращает список всех карт в системе")
    public ResponseEntity<Collection<Card>> getAllCards() {
        return ResponseEntity.ok(administratorClientManagment.getAllCards());
    }

    @GetMapping("/{cardId}")
    @Operation(summary = "Получить карту по ID", description = "Возвращает информацию о карте по её идентификатору")
    public ResponseEntity<Card> getCardById(@PathVariable UUID cardId) {
        return ResponseEntity.ok(administratorClientManagment.getCardById(cardId));
    }

    @PutMapping("/{cardId}/block")
    @Operation(summary = "Заблокировать карту", description = "Устанавливает статус карты в BLOCKED")
    public ResponseEntity<Card> blockCard(@PathVariable UUID cardId) {
        return ResponseEntity.ok(administratorClientManagment.blockCard(cardId));
    }

    @PutMapping("/{cardId}/activate")
    @Operation(summary = "Активировать карту", description = "Устанавливает статус карты в ACTIVE")
    public ResponseEntity<Card> activateCard(@PathVariable UUID cardId) {
        return ResponseEntity.ok(administratorClientManagment.activateCard(cardId));
    }

    @PutMapping("/{cardId}/status")
    @Operation(summary = "Установить статус карты", description = "Устанавливает указанный статус для карты (ACTIVE, BLOCKED, EXPIRED)")
    public ResponseEntity<Card> setCardStatus(@PathVariable UUID cardId, @RequestParam CardStatus status) {
        return ResponseEntity.ok(administratorClientManagment.setCardStatus(cardId, status));
    }

    @DeleteMapping("/{cardId}")
    @Operation(summary = "Удалить карту", description = "Удаляет карту из системы по её идентификатору")
    public ResponseEntity<Void> deleteCard(@PathVariable UUID cardId) {
        administratorClientManagment.deleteCard(cardId);
        return ResponseEntity.noContent().build();
    }
}
