package com.example.bankcards.transaction.infrastructure.primary;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.bankcards.transaction.domain.ClientAccount;
import com.example.bankcards.transaction.infrastructure.secondary.AdministratorClientManagment;

@RestController
@RequestMapping("/api/admin/management-users")
public class AdministratorController {

    private final AdministratorClientManagment administratorClientManagment;

    public AdministratorController(AdministratorClientManagment administratorClientManagment) {
        this.administratorClientManagment = administratorClientManagment;
    }

    @PostMapping("/create-user")
    public ResponseEntity<ClientAccount> createUser(@RequestBody String ownerName) {
        return ResponseEntity.ok(administratorClientManagment.createClientAccount(ownerName));
    }

    @GetMapping("/get-user/{ownerName}")
    public ResponseEntity<ClientAccount> getUser(@PathVariable String ownerName) {
        return ResponseEntity.ok(administratorClientManagment.getClientAccount(ownerName));
    }

}