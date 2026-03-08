package com.example.bankcards.transaction.infrastructure.secondary;

import java.util.Collection;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankcards.shared.error.domain.Assert;
import com.example.bankcards.transaction.application.ClientMapper;
import com.example.bankcards.transaction.domain.ClientAccount;
import com.example.bankcards.transaction.domain.card.Card;
import com.example.bankcards.transaction.domain.card.dto.ClientAccountWithCard;
import com.example.bankcards.transaction.domain.card.params.CardStatus;

@Service
public class AdministratorClientManagment {
    private final ClientAccountRepository repository;
    private final CardRepository cardRepository;
    private final ClientMapper mapper;

    public AdministratorClientManagment(ClientAccountRepository repository, CardRepository cardRepository,
            ClientMapper mapper) {
        this.repository = repository;
        this.cardRepository = cardRepository;
        this.mapper = mapper;
    }

    @Transactional
    public ClientAccount createClientAccount(String ownerName) {
        Assert.field("ownerName", ownerName).notNull().notBlank();
        ClientAccount clientAccount = new ClientAccount(ownerName);
        return repository.save(clientAccount);
    }

    @Transactional
    public ClientAccount getClientAccount(String ownerName) {
        Assert.field("ownerName", ownerName).notNull().notBlank();
        ClientAccount clientAccount = repository.findByOwnerName(ownerName);
        Assert.notNull("clientAccount", clientAccount);
        return clientAccount;
    }

    @Transactional
    public void publishCardforClient(String ownerName) {
        ClientAccount clientAccount = getClientAccount(ownerName);
        Card card = new Card(clientAccount.getUserId());
        card.setAccount(clientAccount);
        cardRepository.save(card);
    }

    @Transactional
    public ClientAccountWithCard getClientAccountWithCards(String ownerName) {
        return mapper.fromClientToClientWithCards()
                .apply(getClientAccount(ownerName));
    }

    @Transactional
    public Collection<Card> getAllCards() {
        return cardRepository.findAll();
    }

    @Transactional
    public Card getCardById(UUID cardId) {
        Assert.notNull("cardId", cardId);
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found with id: " + cardId));
    }

    @Transactional
    public Card blockCard(UUID cardId) {
        Card card = getCardById(cardId);
        card.block();
        return cardRepository.save(card);
    }

    @Transactional
    public Card activateCard(UUID cardId) {
        Card card = getCardById(cardId);
        card.activate();
        return cardRepository.save(card);
    }

    @Transactional
    public void deleteCard(UUID cardId) {
        Assert.notNull("cardId", cardId);
        if (!cardRepository.existsById(cardId)) {
            throw new IllegalArgumentException("Card not found with id: " + cardId);
        }
        cardRepository.deleteById(cardId);
    }

    @Transactional
    public Collection<ClientAccount> getAllClients() {
        return repository.findAll();
    }

    @Transactional
    public void deleteClient(UUID clientId) {
        Assert.notNull("clientId", clientId);
        if (!repository.existsById(clientId)) {
            throw new IllegalArgumentException("Client not found with id: " + clientId);
        }
        repository.deleteById(clientId);
    }

    @Transactional
    public Card setCardStatus(UUID cardId, CardStatus status) {
        Card card = getCardById(cardId);
        Assert.notNull("status", status);
        card.setStatus(status);
        return cardRepository.save(card);
    }
}
