package com.example.bankcards.transaction.infrastructure.primary;

import org.springframework.web.bind.annotation.*;

import com.example.bankcards.transaction.domain.ClientAccount;
import com.example.bankcards.transaction.infrastructure.secondary.AdministratorClientManagment;

@RestController("/api/admin/management-users")
public class AdministratorController {

    private final AdministratorClientManagment administratorClientManagment;

    public AdministratorController(AdministratorClientManagment administratorClientManagment) {
        this.administratorClientManagment = administratorClientManagment;
    }

    @PutMapping("/create-user")
    public ClientAccount createUser(@RequestBody String ownerName) {
        return administratorClientManagment.createAccount(ownerName);
    }
}