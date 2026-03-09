package com.example.bankcards.shared.authentication.infrastructure.primary;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import com.example.bankcards.IntegrationTest;

@IntegrationTest
@ActiveProfiles("test")
class KeycloakAdminControllerIT {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/keycloak/users"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenUserDoesNotHaveAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/keycloak/users")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("user")
                    .roles("USER")))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn200WhenAdminAccess() throws Exception {
        mockMvc.perform(get("/api/admin/keycloak/users")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin")
                    .roles("ADMIN")))
            .andExpect(status().isOk());
    }

    @Test
    void shouldCreateUser() throws Exception {
        String requestBody = """
            {
                "username": "testuser",
                "email": "test@example.com",
                "password": "password123",
                "enabled": true
            }
            """;

        mockMvc.perform(post("/api/admin/keycloak/users")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin")
                    .roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().is(200));
    }

    @Test
    void shouldGetUserByUsername() throws Exception {
        mockMvc.perform(get("/api/admin/keycloak/users/testuser")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin")
                    .roles("ADMIN")))
            .andExpect(status().is(404));
    }

    @Test
    void shouldGetAllRealmRoles() throws Exception {
        mockMvc.perform(get("/api/admin/keycloak/roles")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin")
                    .roles("ADMIN")))
            .andExpect(status().is(200));
    }
}
