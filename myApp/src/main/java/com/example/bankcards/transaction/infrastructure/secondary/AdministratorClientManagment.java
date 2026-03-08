package com.example.bankcards.transaction.infrastructure.secondary;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankcards.shared.error.domain.Assert;
import com.example.bankcards.transaction.application.ClientMapper;
import com.example.bankcards.transaction.domain.ClientAccount;
import com.example.bankcards.transaction.domain.card.Card;
import com.example.bankcards.transaction.domain.card.dto.ClientAccountWithCard;

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
        ClientAccount clientAccount = new ClientAccount(ownerName);
        return repository.save(clientAccount);

    }

    @Transactional
    public ClientAccount getClientAccount(String ownerName) {
        Assert.field("getClientAccountByOwnerName", ownerName).notNull().notBlank();
        ClientAccount clientAccount = repository.findByOwnerName(ownerName);
        // FIXME: Need Err Handler without stackTrace:
        // h [] threw exception [Request processing failed:
        // java.lang.NullPointerException: Cannot invoke
        // "com.example.bankcards.transaction.domain.ClientAccount.getOwnerName()"
        // because "clientAccount" is null] with root cause
        Assert.notBlank("getClientAccountBy", clientAccount.getOwnerName());
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

}
