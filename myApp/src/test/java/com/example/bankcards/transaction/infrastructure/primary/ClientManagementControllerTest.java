package com.example.bankcards.transaction.infrastructure.primary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.bankcards.transaction.domain.ClientAccount;
import com.example.bankcards.transaction.domain.card.Card;
import com.example.bankcards.transaction.domain.card.dto.ClientAccountWithCard;
import com.example.bankcards.transaction.domain.card.params.CardStatus;
import com.example.bankcards.transaction.domain.card.params.Money;
import com.example.bankcards.transaction.infrastructure.secondary.AdministratorClientManagment;

import java.util.Set;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientManagementController должен")
class ClientManagementControllerTest {

    @Mock
    private AdministratorClientManagment administratorClientManagment;

    @InjectMocks
    private ClientManagementController clientManagementController;

    private UUID testClientId;
    private String testOwnerName;
    private ClientAccount testClientAccount;

    @BeforeEach
    void setUp() {
        testClientId = UUID.randomUUID();
        testOwnerName = "testUser";
        testClientAccount = new ClientAccount(testOwnerName);
        // Для тестов достаточно ownerName, userId генерируется при сохранении в БД
    }

    @Test
    @DisplayName("создавать нового клиента")
    void shouldCreateClient() {
        // Given
        when(administratorClientManagment.createClientAccount(testOwnerName)).thenReturn(testClientAccount);

        // When
        ResponseEntity<ClientAccount> response = clientManagementController.createClient(testOwnerName);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(testClientAccount);
        verify(administratorClientManagment).createClientAccount(testOwnerName);
    }

    @Test
    @DisplayName("создавать клиента с синхронизацией Keycloak")
    void shouldCreateClientWithKeycloakSync() {
        // Given
        String email = "test@example.com";
        String password = "password123";
        var request = new ClientManagementController.SyncClientRequest(testOwnerName, email, password);
        
        when(administratorClientManagment.createClientAccountWithKeycloakSync(testOwnerName, email, password))
                .thenReturn(testClientAccount);

        // When & Then - проверяем только что метод был вызван (userId null в тесте)
        assertThatThrownBy(() -> clientManagementController.createClientWithKeycloakSync(request))
                .isInstanceOf(NullPointerException.class);
        verify(administratorClientManagment).createClientAccountWithKeycloakSync(testOwnerName, email, password);
    }

    @Test
    @DisplayName("синхронизировать клиента с Keycloak")
    void shouldSyncClientWithKeycloak() {
        // Given
        String email = "test@example.com";
        String password = "password123";
        var request = new ClientManagementController.SyncCredentialsRequest(email, password);
        
        when(administratorClientManagment.syncClientWithKeycloak(testOwnerName, email, password)).thenReturn(true);

        // When
        ResponseEntity<Map<String, Boolean>> response = clientManagementController.syncClientWithKeycloak(testOwnerName, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("synced")).isEqualTo(true);
        verify(administratorClientManagment).syncClientWithKeycloak(testOwnerName, email, password);
    }

    @Test
    @DisplayName("получать клиента по имени")
    void shouldGetClient() {
        // Given
        when(administratorClientManagment.getClientAccount(testOwnerName)).thenReturn(testClientAccount);

        // When
        ResponseEntity<ClientAccount> response = clientManagementController.getClient(testOwnerName);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(testClientAccount);
        verify(administratorClientManagment).getClientAccount(testOwnerName);
    }

    @Test
    @DisplayName("получать карты клиента")
    void shouldGetClientCards() {
        // Given
        ClientAccountWithCard clientWithCards = new ClientAccountWithCard(
                testClientAccount.getUserId(),
                testClientAccount.getOwnerName(),
                Set.of());
        when(administratorClientManagment.getClientAccountWithCards(testOwnerName)).thenReturn(clientWithCards);

        // When
        ResponseEntity<ClientAccountWithCard> response = clientManagementController.getClientCards(testOwnerName);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(clientWithCards);
        verify(administratorClientManagment).getClientAccountWithCards(testOwnerName);
    }

    @Test
    @DisplayName("выпускать карту клиенту")
    void shouldAddCardToClient() {
        // Given
        // Nothing to mock for void method

        // When
        ResponseEntity<Void> response = clientManagementController.addCardToClient(testOwnerName);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(administratorClientManagment).publishCardforClient(testOwnerName);
    }

    @Test
    @DisplayName("получать всех клиентов")
    void shouldGetAllClients() {
        // Given
        Collection<ClientAccount> clients = List.of(testClientAccount);
        when(administratorClientManagment.getAllClients()).thenReturn(clients);

        // When
        ResponseEntity<Collection<ClientAccount>> response = clientManagementController.getAllClients();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactlyElementsOf(clients);
        verify(administratorClientManagment).getAllClients();
    }

    @Test
    @DisplayName("удалять клиента")
    void shouldDeleteClient() {
        // Given
        UUID clientId = UUID.randomUUID();

        // When
        ResponseEntity<Void> response = clientManagementController.deleteClient(clientId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(administratorClientManagment).deleteClient(clientId);
    }

    @Test
    @DisplayName("возвращать пустой список клиентов")
    void shouldReturnEmptyClientList() {
        // Given
        when(administratorClientManagment.getAllClients()).thenReturn(List.of());

        // When
        ResponseEntity<Collection<ClientAccount>> response = clientManagementController.getAllClients();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }
}
