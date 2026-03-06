package com.example.bankcards.transaction.infrastructure.secondary;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankcards.shared.error.domain.Assert;
import com.example.bankcards.transaction.domain.ClientAccount;
import com.example.bankcards.transaction.domain.card.Card;

@Service
public class AdministratorClientManagment {
    private final ClientAccountRepository repository;
    private final CardRepository cardRepository;

    public AdministratorClientManagment(ClientAccountRepository repository, CardRepository cardRepository) {
        this.repository = repository;
        this.cardRepository = cardRepository;
    }

    @Transactional
    public ClientAccount createClientAccount(String ownerName) {
        ClientAccount clientAccount = new ClientAccount(ownerName);
        return repository.save(clientAccount);

    }

    @Transactional
    public ClientAccount getClientAccount(String ownerName) {
        Assert.field("getClientAccountByOwnerName", ownerName).notNull().notBlank();
        return repository.findByOwnerName(ownerName);
    }

    @Transactional
    public void publishCardforClient(String ownerName) {
        ClientAccount clientAccount = getClientAccount(ownerName);
        cardRepository.save(new Card(clientAccount.getUserId()));
    }

}
