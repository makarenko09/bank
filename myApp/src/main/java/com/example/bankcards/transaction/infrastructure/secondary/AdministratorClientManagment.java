package com.example.bankcards.transaction.infrastructure.secondary;

import org.springframework.stereotype.Service;

import com.example.bankcards.shared.error.domain.Assert;
import com.example.bankcards.transaction.application.ActivatedCard;
import com.example.bankcards.transaction.domain.ClientAccount;

@Service
public class AdministratorClientManagment {
    private final ActivatedCard activatedCard;

    public AdministratorClientManagment(ActivatedCard activatedCard) {
        this.activatedCard = activatedCard;
    }

    public ClientAccount createAccount(String ownerName) {
        ClientAccount client = ClientAccount.ClientAccountBuilder(ownerName);
        Assert.field("newCreatedClietnAccount", client.getOwnerName()).equals(ownerName);

        activatedCard.addUserNewCard(client);

        return client;
    }

}
