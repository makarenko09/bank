package com.example.bankcards.transaction.infrastructure.secondary;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankcards.shared.authentication.application.AuthenticatedUser;
import com.example.bankcards.shared.error.domain.Assert;
import com.example.bankcards.transaction.domain.ClientAccount;
import com.example.bankcards.transaction.domain.card.Card;
import com.example.bankcards.transaction.domain.card.params.Money;

@Service
public class UserCardManagement {

    private final CardRepository cardRepository;
    private final ClientAccountRepository clientAccountRepository;

    public UserCardManagement(CardRepository cardRepository, ClientAccountRepository clientAccountRepository) {
        this.cardRepository = cardRepository;
        this.clientAccountRepository = clientAccountRepository;
    }

    /**
     * Получить все карты текущего пользователя
     */
    @Transactional(readOnly = true)
    public Collection<Card> getUserCards() {
        String username = AuthenticatedUser.username().get();
        ClientAccount account = clientAccountRepository.findByOwnerName(username);
        Assert.notNull("account", account);
        return cardRepository.findAllByClientId(account.getUserId());
    }

    /**
     * Получить карты пользователя с пагинацией
     */
    @Transactional(readOnly = true)
    public Page<Card> getUserCardsPaginated(Pageable pageable) {
        String username = AuthenticatedUser.username().get();
        ClientAccount account = clientAccountRepository.findByOwnerName(username);
        Assert.notNull("account", account);
        List<Card> allCards = cardRepository.findAllByClientId(account.getUserId()).stream().toList();
        
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allCards.size());
        
        if (start >= allCards.size()) {
            return new PageImpl<>(allCards, pageable, allCards.size());
        }
        
        return new PageImpl<>(allCards.subList(start, end), pageable, allCards.size());
    }

    /**
     * Получить баланс карты
     */
    @Transactional(readOnly = true)
    public Money getCardBalance(UUID cardId) {
        Card card = getCardById(cardId);
        validateCardOwnership(card);
        return card.getBalance();
    }

    /**
     * Запросить блокировку карты
     */
    @Transactional
    public Card requestBlockCard(UUID cardId) {
        Card card = getCardById(cardId);
        validateCardOwnership(card);
        card.block();
        return cardRepository.save(card);
    }

    /**
     * Перевод между своими картами
     */
    @Transactional
    public void transferBetweenCards(UUID fromCardId, UUID toCardId, BigDecimal amount) {
        Assert.notNull("fromCardId", fromCardId);
        Assert.notNull("toCardId", toCardId);
        Assert.notNull("amount", amount);
        Assert.field("amount", amount).positive();

        Card fromCard = getCardById(fromCardId);
        Card toCard = getCardById(toCardId);

        // Проверка принадлежности карт одному пользователю
        validateCardOwnership(fromCard);
        validateCardOwnership(toCard);

        if (fromCard.getId().equals(toCard.getId())) {
            throw new IllegalArgumentException("Cannot transfer to the same card");
        }

        if (fromCard.getBalance().amount().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds on card: " + fromCard.maskedNumber());
        }

        // Списание с карты-отправителя
        BigDecimal newFromBalance = fromCard.getBalance().amount().subtract(amount);
        fromCard.setBalance(new Money(newFromBalance));

        // Зачисление на карту-получателя
        BigDecimal newToBalance = toCard.getBalance().amount().add(amount);
        toCard.setBalance(new Money(newToBalance));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);
    }

    /**
     * Получить карту по ID с проверкой принадлежности пользователю
     */
    private Card getCardById(UUID cardId) {
        Assert.notNull("cardId", cardId);
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardId));
    }

    /**
     * Проверить, что карта принадлежит текущему пользователю
     */
    private void validateCardOwnership(Card card) {
        String username = AuthenticatedUser.username().get();
        ClientAccount account = clientAccountRepository.findByOwnerName(username);
        Assert.notNull("account", account);

        if (!card.getAccount().getUserId().equals(account.getUserId())) {
            throw new SecurityException("Card does not belong to the current user");
        }
    }
}
