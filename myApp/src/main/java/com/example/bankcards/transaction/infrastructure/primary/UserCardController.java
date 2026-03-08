package com.example.bankcards.transaction.infrastructure.primary;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.bankcards.transaction.domain.card.Card;
import com.example.bankcards.transaction.domain.card.dto.CardTransferRequest;
import com.example.bankcards.transaction.domain.card.params.Money;
import com.example.bankcards.transaction.infrastructure.secondary.UserCardManagement;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/user/cards")
@Tag(name = "User Card Management", description = "API для управления своими картами (USER only)")
public class UserCardController {

    private final UserCardManagement userCardManagement;

    public UserCardController(UserCardManagement userCardManagement) {
        this.userCardManagement = userCardManagement;
    }

    @GetMapping
    @Operation(summary = "Получить все свои карты", description = "Возвращает список всех карт текущего пользователя")
    public ResponseEntity<Collection<Card>> getUserCards() {
        return ResponseEntity.ok(userCardManagement.getUserCards());
    }

    @GetMapping("/paginated")
    @Operation(summary = "Получить свои карты с пагинацией", description = "Возвращает карты текущего пользователя с поддержкой пагинации")
    public ResponseEntity<Page<Card>> getUserCardsPaginated(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(userCardManagement.getUserCardsPaginated(pageable));
    }

    @GetMapping("/{cardId}")
    @Operation(summary = "Получить карту по ID", description = "Возвращает информацию о карте текущего пользователя")
    public ResponseEntity<Card> getCard(@PathVariable UUID cardId) {
        return ResponseEntity.ok(userCardManagement.getUserCards().stream()
                .filter(card -> card.getId().equals(cardId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Card not found or access denied")));
    }

    @GetMapping("/{cardId}/balance")
    @Operation(summary = "Получить баланс карты", description = "Возвращает баланс карты текущего пользователя")
    public ResponseEntity<Map<String, Object>> getCardBalance(@PathVariable UUID cardId) {
        Money balance = userCardManagement.getCardBalance(cardId);
        return ResponseEntity.ok(Map.of(
                "cardId", cardId,
                "balance", balance.amount(),
                "currency", "RUB"));
    }

    @PutMapping("/{cardId}/block")
    @Operation(summary = "Запросить блокировку карты", description = "Запрашивает блокировку карты текущего пользователя")
    public ResponseEntity<Card> requestBlockCard(@PathVariable UUID cardId) {
        return ResponseEntity.ok(userCardManagement.requestBlockCard(cardId));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Перевод между своими картами", description = "Выполняет перевод средств между картами текущего пользователя")
    public ResponseEntity<Map<String, Object>> transferBetweenCards(@RequestBody CardTransferRequest request) {
        userCardManagement.transferBetweenCards(
                request.getFromCardId(),
                request.getToCardId(),
                request.getAmount());

        return ResponseEntity.ok(Map.of(
                "message", "Transfer completed successfully",
                "fromCardId", request.getFromCardId(),
                "toCardId", request.getToCardId(),
                "amount", request.getAmount()));
    }

    @GetMapping("/{cardId}/masked-number")
    @Operation(summary = "Получить маскированный номер карты", description = "Возвращает маскированный номер карты (**** **** **** 1234)")
    public ResponseEntity<Map<String, String>> getCardMaskedNumber(@PathVariable UUID cardId) {
        Card card = userCardManagement.getUserCards().stream()
                .filter(c -> c.getId().equals(cardId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Card not found or access denied"));

        return ResponseEntity.ok(Map.of(
                "cardId", cardId.toString(),
                "maskedNumber", card.maskedNumber()));
    }
}
