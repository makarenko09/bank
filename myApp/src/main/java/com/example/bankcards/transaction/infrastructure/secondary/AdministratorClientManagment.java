package com.example.bankcards.transaction.infrastructure.secondary;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankcards.transaction.domain.ClientAccount;

@Service
public class AdministratorClientManagment {
    private final ClientAccountRepository repository;

    public AdministratorClientManagment(ClientAccountRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ClientAccount createClientAccount(String ownerName) {
        ClientAccount clientAccount = new ClientAccount(ownerName);
        return repository.save(clientAccount);

    }

}
