package com.example.bankcards.shared.authentication.infrastructure.primary;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.annotation.Validated;

@Validated
@Primary
@ConfigurationProperties(prefix = "keycloak.admin", ignoreUnknownFields = false)
public class KeycloakAdminProperties {

    /**
     * URL сервера Keycloak (например, http://localhost:9081)
     */
    @NotNull
    private String serverUrl;

    /**
     * Административный реалм (обычно master)
     */
    @NotNull
    private String adminRealm = "master";

    /**
     * Реалм для управления (например, seed4j)
     */
    @NotNull
    private String realm;

    /**
     * Client ID для административного доступа (admin-cli или dedicated client)
     */
    @NotNull
    private String clientId;

    /**
     * Client Secret для административного доступа (для client_credentials)
     */
    private String clientSecret;

    /**
     * Username для административного доступа (для password grant)
     */
    private String username;

    /**
     * Password для административного доступа (для password grant)
     */
    private String password;

    /**
     * Тип гранта (client_credentials или password)
     */
    @NotNull
    private String grantType = "client_credentials";

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getAdminRealm() {
        return adminRealm;
    }

    public void setAdminRealm(String adminRealm) {
        this.adminRealm = adminRealm;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }
}
