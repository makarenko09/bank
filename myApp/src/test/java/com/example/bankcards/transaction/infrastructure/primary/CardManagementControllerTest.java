package com.example.bankcards.transaction.infrastructure.primary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.bankcards.transaction.domain.card.Card;
import com.example.bankcards.transaction.domain.card.params.CardStatus;
import com.example.bankcards.transaction.domain.card.params.Money;
import com.example.bankcards.transaction.infrastructure.secondary.AdministratorClientManagment;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardManagementController должен")
class CardManagementControllerTest {

    @Mock
    private AdministratorClientManagment administratorClientManagment;

    @InjectMocks
    private CardManagementController cardManagementController;

    private UUID testCardId;
    private Card testCard;

    @BeforeEach
    void setUp() {
        testCardId = UUID.randomUUID();
        testCard = createTestCard(testCardId, CardStatus.ACTIVE);
    }

    private Card createTestCard(UUID id, CardStatus status) {
        Card card = new Card();
        card.setStatus(status);
        card.setBalance(new Money(BigDecimal.valueOf(1000.00)));
        // Используем reflection или конструктор для установки ID, если нужно
        // В данном случае ID генерируется автоматически, но для теста мы можем
        // проверить карту через мокирование сервиса
        return card;
    }

    @Test
    @DisplayName("возвращать все карты из системы")
    void shouldGetAllCards() {
        // Given
        Collection<Card> cards = List.of(testCard);
        when(administratorClientManagment.getAllCards()).thenReturn(cards);

        // When
        ResponseEntity<Collection<Card>> response = cardManagementController.getAllCards();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactlyElementsOf(cards);
        verify(administratorClientManagment).getAllCards();
    }

    @Test
    @DisplayName("возвращать карту по ID")
    void shouldGetCardById() {
        // Given
        when(administratorClientManagment.getCardById(testCardId)).thenReturn(testCard);

        // When
        ResponseEntity<Card> response = cardManagementController.getCardById(testCardId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(testCard);
        verify(administratorClientManagment).getCardById(testCardId);
    }

    @Test
    @DisplayName("блокировать карту по ID")
    void shouldBlockCard() {
        // Given
        Card blockedCard = createTestCard(testCardId, CardStatus.BLOCKED);
        when(administratorClientManagment.blockCard(testCardId)).thenReturn(blockedCard);

        // When
        ResponseEntity<Card> response = cardManagementController.blockCard(testCardId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status()).isEqualTo(CardStatus.BLOCKED);
        verify(administratorClientManagment).blockCard(testCardId);
    }

    @Test
    @DisplayName("активировать карту по ID")
    void shouldActivateCard() {
        // Given
        Card activatedCard = createTestCard(testCardId, CardStatus.ACTIVE);
        when(administratorClientManagment.activateCard(testCardId)).thenReturn(activatedCard);

        // When
        ResponseEntity<Card> response = cardManagementController.activateCard(testCardId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status()).isEqualTo(CardStatus.ACTIVE);
        verify(administratorClientManagment).activateCard(testCardId);
    }

    @Test
    @DisplayName("устанавливать статус карты")
    void shouldSetCardStatus() {
        // Given
        CardStatus newStatus = CardStatus.EXPIRED;
        Card expiredCard = createTestCard(testCardId, CardStatus.EXPIRED);
        when(administratorClientManagment.setCardStatus(testCardId, newStatus)).thenReturn(expiredCard);

        // When
        ResponseEntity<Card> response = cardManagementController.setCardStatus(testCardId, newStatus);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status()).isEqualTo(CardStatus.EXPIRED);
        verify(administratorClientManagment).setCardStatus(testCardId, newStatus);
    }

    @Test
    @DisplayName("удалять карту по ID")
    void shouldDeleteCard() {
        // Given
        // Nothing to mock for void method

        // When
        ResponseEntity<Void> response = cardManagementController.deleteCard(testCardId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(administratorClientManagment).deleteCard(testCardId);
    }

    @Test
    @DisplayName("возвращать пустой список когда карт нет")
    void shouldReturnEmptyListWhenNoCards() {
        // Given
        when(administratorClientManagment.getAllCards()).thenReturn(List.of());

        // When
        ResponseEntity<Collection<Card>> response = cardManagementController.getAllCards();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    @DisplayName("обрабатывать все статусы карт при установке статуса")
    void shouldHandleAllCardStatuses() {
        // Given
        for (CardStatus status : CardStatus.values()) {
            Card cardWithStatus = createTestCard(testCardId, status);
            when(administratorClientManagment.setCardStatus(testCardId, status)).thenReturn(cardWithStatus);

            // When
            ResponseEntity<Card> response = cardManagementController.setCardStatus(testCardId, status);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().status()).isEqualTo(status);
        }
    }
}
