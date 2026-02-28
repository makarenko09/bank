package com.example.bankcards.transaction.infrastructure.secondary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.bankcards.shared.error.domain.Assert;
import com.example.bankcards.transaction.application.ActivatedCard;
import com.example.bankcards.transaction.domain.ClientAccount;
import com.example.bankcards.transaction.domain.card.Card;

@Service
public class AdministratorClientManagment {

    private final ActivatedCard activatedCard;

    public AdministratorClientManagment(ActivatedCard activatedCard, CardRepository repository) {
        this.activatedCard = activatedCard;
        this.repository = repository;
    }

    private final Logger logger = LoggerFactory.getLogger(AdministratorClientManagment.class);
    private final CardRepository repository;

    public ClientAccount createAccount(String ownerName) {
        logger.bug("ownmer - {}", ownerName);

        ClientAccount clientaccount = ClientAccount.ClientAccountBuilder(ownerName);
        // Assert.field("newCreatedClietnAccount",
        // clientaccount.getOwnerName()).equals(ownerName);

        Card userWithNewCard = activatedCard.addUserNewCard(clientaccount);
        repository.save(userWithNewCard);

        return clientaccount;
    }

}
