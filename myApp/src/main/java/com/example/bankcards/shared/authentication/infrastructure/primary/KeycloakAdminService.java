package com.example.bankcards.shared.authentication.infrastructure.primary;

import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;
import com.example.bankcards.shared.error.domain.Assert;

@Service
public class KeycloakAdminService {

    private final Keycloak keycloak;
    private final RealmResource realmResource;
    private final KeycloakAdminProperties properties;

    public KeycloakAdminService(Keycloak keycloak, RealmResource realmResource, KeycloakAdminProperties properties) {
        this.keycloak = keycloak;
        this.realmResource = realmResource;
        this.properties = properties;
    }

    /**
     * Создать пользователя в Keycloak
     */
    public String createUser(String username, String email, String password, boolean enabled) {
        Assert.field("username", username).notNull().notBlank();
        Assert.field("email", email).notNull().notBlank();
        Assert.field("password", password).notNull().notBlank();

        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setEnabled(enabled);
        user.setEmailVerified(false);

        Response response = realmResource.users().create(user);
        int status = response.getStatus();
        
        if (status == 201) {
            String userId = extractUserId(response);
            response.close();
            
            // Установить пароль
            setPassword(userId, password);
            return userId;
        }
        
        // Получить детальную ошибку от Keycloak
        String errorBody = response.hasEntity() ? response.readEntity(String.class) : "No details";
        response.close();
        
        throw new RuntimeException(String.format(
            "Failed to create user in Keycloak (status %d): %s. Username: %s, Email: %s",
            status, errorBody, username, email
        ));
    }

    /**
     * Установить пароль пользователю
     */
    public void setPassword(String userId, String password) {
        Assert.field("userId", userId).notNull().notBlank();
        Assert.field("password", password).notNull().notBlank();

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);

        realmResource.users().get(userId).resetPassword(credential);
    }

    /**
     * Назначить роль пользователю
     */
    public void assignRole(String userId, String roleName) {
        Assert.field("userId", userId).notNull().notBlank();
        Assert.field("roleName", roleName).notNull().notBlank();

        RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
        realmResource.users().get(userId).roles().realmLevel().add(List.of(role));
    }

    /**
     * Получить пользователя по username
     */
    public Optional<UserRepresentation> getUserByUsername(String username) {
        Assert.field("username", username).notNull().notBlank();

        List<UserRepresentation> users = realmResource.users().search(username);
        if (users == null || users.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(users.get(0));
    }

    /**
     * Получить пользователя по ID
     */
    public Optional<UserRepresentation> getUserById(String userId) {
        Assert.field("userId", userId).notNull().notBlank();

        try {
            UserRepresentation user = realmResource.users().get(userId).toRepresentation();
            return Optional.of(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Удалить пользователя
     */
    public void deleteUser(String userId) {
        Assert.field("userId", userId).notNull().notBlank();
        realmResource.users().delete(userId);
    }

    /**
     * Заблокировать пользователя
     */
    public void disableUser(String userId) {
        Assert.field("userId", userId).notNull().notBlank();
        UserRepresentation user = realmResource.users().get(userId).toRepresentation();
        user.setEnabled(false);
        realmResource.users().get(userId).update(user);
    }

    /**
     * Активировать пользователя
     */
    public void enableUser(String userId) {
        Assert.field("userId", userId).notNull().notBlank();
        UserRepresentation user = realmResource.users().get(userId).toRepresentation();
        user.setEnabled(true);
        realmResource.users().get(userId).update(user);
    }

    /**
     * Получить роли пользователя
     */
    public List<RoleRepresentation> getUserRoles(String userId) {
        Assert.field("userId", userId).notNull().notBlank();
        return realmResource.users().get(userId).roles().realmLevel().listAll();
    }

    /**
     * Отозвать роль у пользователя
     */
    public void revokeRole(String userId, String roleName) {
        Assert.field("userId", userId).notNull().notBlank();
        Assert.field("roleName", roleName).notNull().notBlank();

        RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
        realmResource.users().get(userId).roles().realmLevel().remove(List.of(role));
    }

    /**
     * Создать роль в реалме
     */
    public void createRole(String roleName, String description) {
        Assert.field("roleName", roleName).notNull().notBlank();

        RoleRepresentation role = new RoleRepresentation();
        role.setName(roleName);
        role.setDescription(description);
        realmResource.roles().create(role);
    }

    /**
     * Получить роль по имени
     */
    public Optional<RoleRepresentation> getRole(String roleName) {
        Assert.field("roleName", roleName).notNull().notBlank();

        try {
            RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
            return Optional.of(role);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Получить все роли реалма
     */
    public List<RoleRepresentation> getAllRealmRoles() {
        return realmResource.roles().list();
    }

    /**
     * Получить всех пользователей реалма
     */
    public List<UserRepresentation> getAllUsers() {
        return realmResource.users().list();
    }

    /**
     * Проверить существует ли пользователь
     */
    public boolean userExists(String username) {
        return getUserByUsername(username).isPresent();
    }

    private String extractUserId(Response response) {
        String location = response.getHeaderString("Location");
        if (location == null) {
            throw new RuntimeException("Location header not found in create user response");
        }
        return location.substring(location.lastIndexOf("/") + 1);
    }
}
