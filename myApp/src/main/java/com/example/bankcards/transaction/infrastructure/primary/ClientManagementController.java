package com.example.bankcards.transaction.infrastructure.primary;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import com.example.bankcards.shared.authentication.domain.Role;
import com.example.bankcards.transaction.domain.ClientAccount;
import com.example.bankcards.transaction.domain.card.dto.ClientAccountWithCard;
import com.example.bankcards.transaction.infrastructure.secondary.AdministratorClientManagment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/admin/clients")
@Tag(name = "Client Management", description = "API для управления клиентами (ADMIN only)")
@Secured("ROLE_ADMIN")
public class ClientManagementController {

    private final AdministratorClientManagment administratorClientManagment;

    public ClientManagementController(AdministratorClientManagment administratorClientManagment) {
        this.administratorClientManagment = administratorClientManagment;
    }

    @PostMapping
    @Operation(summary = "Создать нового клиента", description = "Создает новый клиентский аккаунт с указанным именем владельца")
    public ResponseEntity<ClientAccount> createClient(@RequestBody String ownerName) {
        return ResponseEntity.ok(administratorClientManagment.createClientAccount(ownerName));
    }

    /**
     * Создать клиента с синхронизацией Keycloak.
     * POST /api/admin/clients/sync
     * {
     *   "ownerName": "john",
     *   "email": "john@example.com",
     *   "password": "secret123"
     * }
     */
    @PostMapping("/sync")
    @Operation(
        summary = "Создать клиента с синхронизацией Keycloak",
        description = "Создает клиента в PostgreSQL и синхронизирует с Keycloak (username = ownerName)"
    )
    public ResponseEntity<Map<String, Object>> createClientWithKeycloakSync(
            @RequestBody @Valid SyncClientRequest request) {
        ClientAccount account = administratorClientManagment.createClientAccountWithKeycloakSync(
            request.ownerName(),
            request.email(),
            request.password()
        );
        return ResponseEntity.ok(Map.of(
            "userId", account.getUserId().toString(),
            "ownerName", account.getOwnerName(),
            "synced", true
        ));
    }

    /**
     * Синхронизировать существующего клиента с Keycloak.
     * POST /api/admin/clients/{ownerName}/sync
     */
    @PostMapping("/{ownerName}/sync")
    @Operation(
        summary = "Синхронизировать существующего клиента с Keycloak",
        description = "Если клиент есть в PostgreSQL, но нет в Keycloak — создаст в Keycloak"
    )
    public ResponseEntity<Map<String, Boolean>> syncClientWithKeycloak(
            @PathVariable String ownerName,
            @RequestBody @Valid SyncCredentialsRequest request) {
        boolean synced = administratorClientManagment.syncClientWithKeycloak(
            ownerName,
            request.email(),
            request.password()
        );
        return ResponseEntity.ok(Map.of("synced", synced));
    }

    @GetMapping("/{ownerName}")
    @Operation(summary = "Получить клиента по имени", description = "Возвращает информацию о клиенте по имени владельца")
    public ResponseEntity<ClientAccount> getClient(@PathVariable String ownerName) {
        return ResponseEntity.ok(administratorClientManagment.getClientAccount(ownerName));
    }

    @GetMapping("/{ownerName}/cards")
    @Operation(summary = "Получить карты клиента", description = "Возвращает информацию о клиенте со всеми его картами")
    public ResponseEntity<ClientAccountWithCard> getClientCards(@PathVariable String ownerName) {
        return ResponseEntity.ok(administratorClientManagment.getClientAccountWithCards(ownerName));
    }

    @PutMapping("/{ownerName}/add-card")
    @Operation(summary = "Выпустить карту клиенту", description = "Создает и привязывает новую карту к аккаунту клиента")
    public ResponseEntity<Void> addCardToClient(@PathVariable String ownerName) {
        administratorClientManagment.publishCardforClient(ownerName);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @Operation(summary = "Получить всех клиентов", description = "Возвращает список всех клиентских аккаунтов")
    public ResponseEntity<Collection<ClientAccount>> getAllClients() {
        return ResponseEntity.ok(administratorClientManagment.getAllClients());
    }

    @DeleteMapping("/{clientId}")
    @Operation(summary = "Удалить клиента", description = "Удаляет клиентский аккаунт и все связанные карты")
    public ResponseEntity<Void> deleteClient(@PathVariable UUID clientId) {
        administratorClientManagment.deleteClient(clientId);
        return ResponseEntity.noContent().build();
    }

    public record SyncClientRequest(
        @NotBlank(message = "ownerName is required") String ownerName,
        @NotBlank(message = "email is required") @Email(message = "email must be valid") String email,
        @NotBlank(message = "password is required") @Size(min = 8, message = "password must be at least 8 characters") String password
    ) {}
    
    public record SyncCredentialsRequest(
        @NotBlank(message = "email is required") @Email(message = "email must be valid") String email,
        @NotBlank(message = "password is required") @Size(min = 8, message = "password must be at least 8 characters") String password
    ) {}
}
