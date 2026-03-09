package com.example.bankcards.shared.authentication.infrastructure.primary;

import java.util.List;
import java.util.Map;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.bankcards.shared.authentication.domain.Role;

@RestController
@RequestMapping("/api/admin/keycloak")
@Secured("ROLE_ADMIN")
public class KeycloakAdminController {

    private final KeycloakAdminService keycloakAdminService;

    public KeycloakAdminController(KeycloakAdminService keycloakAdminService) {
        this.keycloakAdminService = keycloakAdminService;
    }

    /**
     * Создать пользователя в Keycloak
     * POST /api/admin/keycloak/users
     */
    @PostMapping("/users")
    public ResponseEntity<Map<String, String>> createUser(@RequestBody CreateUserRequest request) {
        String userId = keycloakAdminService.createUser(
            request.username(),
            request.email(),
            request.password(),
            request.enabled() != null ? request.enabled() : true
        );
        return ResponseEntity.ok(Map.of("userId", userId, "username", request.username()));
    }

    /**
     * Получить пользователя по username
     * GET /api/admin/keycloak/users/{username}
     */
    @GetMapping("/users/{username}")
    public ResponseEntity<UserRepresentation> getUserByUsername(@PathVariable String username) {
        return keycloakAdminService.getUserByUsername(username)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Получить пользователя по ID
     * GET /api/admin/keycloak/users/id/{userId}
     */
    @GetMapping("/users/id/{userId}")
    public ResponseEntity<UserRepresentation> getUserById(@PathVariable String userId) {
        return keycloakAdminService.getUserById(userId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Получить всех пользователей
     * GET /api/admin/keycloak/users
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserRepresentation>> getAllUsers() {
        return ResponseEntity.ok(keycloakAdminService.getAllUsers());
    }

    /**
     * Удалить пользователя
     * DELETE /api/admin/keycloak/users/{userId}
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        keycloakAdminService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Заблокировать пользователя
     * PUT /api/admin/keycloak/users/{userId}/disable
     */
    @PutMapping("/users/{userId}/disable")
    public ResponseEntity<Void> disableUser(@PathVariable String userId) {
        keycloakAdminService.disableUser(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Активировать пользователя
     * PUT /api/admin/keycloak/users/{userId}/enable
     */
    @PutMapping("/users/{userId}/enable")
    public ResponseEntity<Void> enableUser(@PathVariable String userId) {
        keycloakAdminService.enableUser(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Установить пароль пользователю
     * PUT /api/admin/keycloak/users/{userId}/password
     */
    @PutMapping("/users/{userId}/password")
    public ResponseEntity<Void> setPassword(@PathVariable String userId, @RequestBody Map<String, String> body) {
        keycloakAdminService.setPassword(userId, body.get("password"));
        return ResponseEntity.ok().build();
    }

    /**
     * Назначить роль пользователю
     * POST /api/admin/keycloak/users/{userId}/roles/{roleName}
     */
    @PostMapping("/users/{userId}/roles/{roleName}")
    public ResponseEntity<Void> assignRole(@PathVariable String userId, @PathVariable String roleName) {
        keycloakAdminService.assignRole(userId, roleName);
        return ResponseEntity.ok().build();
    }

    /**
     * Отозвать роль у пользователя
     * DELETE /api/admin/keycloak/users/{userId}/roles/{roleName}
     */
    @DeleteMapping("/users/{userId}/roles/{roleName}")
    public ResponseEntity<Void> revokeRole(@PathVariable String userId, @PathVariable String roleName) {
        keycloakAdminService.revokeRole(userId, roleName);
        return ResponseEntity.ok().build();
    }

    /**
     * Получить роли пользователя
     * GET /api/admin/keycloak/users/{userId}/roles
     */
    @GetMapping("/users/{userId}/roles")
    public ResponseEntity<List<RoleRepresentation>> getUserRoles(@PathVariable String userId) {
        return ResponseEntity.ok(keycloakAdminService.getUserRoles(userId));
    }

    /**
     * Создать роль
     * POST /api/admin/keycloak/roles
     */
    @PostMapping("/roles")
    public ResponseEntity<Void> createRole(@RequestBody CreateRoleRequest request) {
        keycloakAdminService.createRole(request.name(), request.description());
        return ResponseEntity.ok().build();
    }

    /**
     * Получить роль по имени
     * GET /api/admin/keycloak/roles/{roleName}
     */
    @GetMapping("/roles/{roleName}")
    public ResponseEntity<RoleRepresentation> getRole(@PathVariable String roleName) {
        return keycloakAdminService.getRole(roleName)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Получить все роли реалма
     * GET /api/admin/keycloak/roles
     */
    @GetMapping("/roles")
    public ResponseEntity<List<RoleRepresentation>> getAllRealmRoles() {
        return ResponseEntity.ok(keycloakAdminService.getAllRealmRoles());
    }

    /**
     * Проверить существует ли пользователь
     * GET /api/admin/keycloak/users/{username}/exists
     */
    @GetMapping("/users/{username}/exists")
    public ResponseEntity<Map<String, Boolean>> userExists(@PathVariable String username) {
        return ResponseEntity.ok(Map.of("exists", keycloakAdminService.userExists(username)));
    }

    public record CreateUserRequest(String username, String email, String password, Boolean enabled) {}
    public record CreateRoleRequest(String name, String description) {}
}
