package com.example.bankcards.transaction.infrastructure.secondary;

import java.util.Collection;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankcards.shared.error.domain.Assert;
import com.example.bankcards.shared.authentication.infrastructure.primary.KeycloakAdminService;
import com.example.bankcards.transaction.application.ClientMapper;
import com.example.bankcards.transaction.domain.ClientAccount;
import com.example.bankcards.transaction.domain.card.Card;
import com.example.bankcards.transaction.domain.card.dto.ClientAccountWithCard;
import com.example.bankcards.transaction.domain.card.params.CardStatus;

/**
 * Сервис административного управления клиентами.
 * <p>
 * Интегрирован с Keycloak для синхронизации пользователей:
 * - PostgreSQL (ClientAccount) — бизнес-данные (счета, карты)
 * - Keycloak — аутентификация, credentials, роли
 * <p>
 * Связь между системами осуществляется по {@code username} (ownerName).
 */
@Service
public class AdministratorClientManagment {
    private final ClientAccountRepository repository;
    private final CardRepository cardRepository;
    private final ClientMapper mapper;
    private final KeycloakAdminService keycloakAdminService;

    public AdministratorClientManagment(ClientAccountRepository repository, CardRepository cardRepository,
            ClientMapper mapper, KeycloakAdminService keycloakAdminService) {
        this.repository = repository;
        this.cardRepository = cardRepository;
        this.mapper = mapper;
        this.keycloakAdminService = keycloakAdminService;
    }

    @Transactional
    public ClientAccount createClientAccount(String ownerName) {
        Assert.field("ownerName", ownerName).notNull().notBlank();
        ClientAccount clientAccount = new ClientAccount(ownerName);
        return repository.save(clientAccount);
    }

    /**
     * Создать клиента в PostgreSQL и синхронизировать с Keycloak.
     * <p>
     * Метод создаёт запись в локальной БД и пользователя в Keycloak с тем же username.
     * Keycloak будет использоваться для аутентификации, PostgreSQL — для бизнес-данных.
     *
     * @param ownerName имя владельца (будет использоваться как username в Keycloak)
     * @param email email пользователя
     * @param password пароль для Keycloak
     * @return сохранённый ClientAccount
     */
    @Transactional
    public ClientAccount createClientAccountWithKeycloakSync(String ownerName, String email, String password) {
        Assert.field("ownerName", ownerName).notNull().notBlank();
        Assert.field("email", email).notNull().notBlank();
        Assert.field("password", password).notNull().notBlank();

        // Сначала создаём в PostgreSQL
        ClientAccount clientAccount = new ClientAccount(ownerName);
        clientAccount = repository.save(clientAccount);

        // Затем синхронизируем с Keycloak
        try {
            keycloakAdminService.createUser(ownerName, email, password, true);
            // Назначаем базовую роль USER
            keycloakAdminService.getUserByUsername(ownerName)
                .ifPresent(user -> {
                    try {
                        keycloakAdminService.assignRole(user.getId(), "ROLE_USER");
                    } catch (Exception e) {
                        // Роль может не существовать, это не критично
                    }
                });
        } catch (Exception e) {
            // Откатываем создание в PostgreSQL если Keycloak failed
            repository.delete(clientAccount);
            throw new RuntimeException("Failed to sync user with Keycloak: " + e.getMessage(), e);
        }

        return clientAccount;
    }

    /**
     * Синхронизировать существующего клиента с Keycloak.
     * <p>
     * Если клиент уже существует в PostgreSQL, но отсутствует в Keycloak,
     * этот метод создаст соответствующую запись в Keycloak.
     *
     * @param ownerName имя владельца
     * @param email email пользователя
     * @param password пароль для Keycloak
     * @return true если синхронизация успешна
     */
    @Transactional
    public boolean syncClientWithKeycloak(String ownerName, String email, String password) {
        Assert.field("ownerName", ownerName).notNull().notBlank();

        // Проверяем существование в PostgreSQL
        ClientAccount clientAccount = getClientAccount(ownerName);
        Assert.notNull("clientAccount", clientAccount);

        // Проверяем, существует ли уже в Keycloak
        if (keycloakAdminService.userExists(ownerName)) {
            return false; // Уже существует
        }

        // Создаём в Keycloak
        keycloakAdminService.createUser(ownerName, email, password, true);
        return true;
    }

    /**
     * Удалить клиента из PostgreSQL и Keycloak.
     * <p>
     * Удаляет запись из обеих систем. Если Keycloak недоступен,
     * запись в PostgreSQL всё равно будет удалена.
     *
     * @param clientId ID клиента в PostgreSQL
     */
    @Transactional
    public void deleteClientWithKeycloakSync(UUID clientId) {
        Assert.notNull("clientId", clientId);

        // Получаем клиента для получения ownerName
        ClientAccount clientAccount = repository.findById(clientId)
            .orElseThrow(() -> new IllegalArgumentException("Client not found with id: " + clientId));

        // Удаляем из PostgreSQL
        repository.deleteById(clientId);

        // Пытаемся удалить из Keycloak (не критично если не получится)
        try {
            keycloakAdminService.getUserByUsername(clientAccount.getOwnerName())
                .ifPresent(user -> keycloakAdminService.deleteUser(user.getId()));
        } catch (Exception e) {
            // Логируем, но не выбрасываем исключение
            // Пользователь уже удалён из PostgreSQL
        }
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
        deleteClientWithKeycloakSync(clientId);
    }

    @Transactional
    public Card setCardStatus(UUID cardId, CardStatus status) {
        Card card = getCardById(cardId);
        Assert.notNull("status", status);
        card.setStatus(status);
        return cardRepository.save(card);
    }
}
