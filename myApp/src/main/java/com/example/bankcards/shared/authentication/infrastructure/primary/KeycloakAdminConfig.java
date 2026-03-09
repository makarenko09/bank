package com.example.bankcards.shared.authentication.infrastructure.primary;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Конфигурация Keycloak Admin Client.
 * <p>
 * ObjectMapper настраивается согласно документации Keycloak:
 * https://www.keycloak.org/securing-apps/admin-client
 * <ul>
 *   <li>NON_NULL - не сериализовать null значения</li>
 *   <li>FAIL_ON_UNKNOWN_PROPERTIES = false - игнорировать неизвестные свойства
 *   (для совместимости между версиями Keycloak)</li>
 * </ul>
 */
@Configuration
@Primary
@EnableConfigurationProperties(KeycloakAdminProperties.class)
public class KeycloakAdminConfig {

    /**
     * Создаёт и настраивает ObjectMapper для Keycloak Admin Client.
     * <p>
     * Настройки согласно официальной документации Keycloak:
     * <pre>{@code
     * objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
     * objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
     * }</pre>
     * <p>
     * Этот бин используется глобально в приложении и автоматически применяется
     * к Keycloak Admin Client через SPI механизм Jackson.
     */
    @Bean
    @Primary
    public ObjectMapper keycloakObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    @Bean
    @Primary
    public Keycloak keycloakAdminClient(KeycloakAdminProperties properties) {
        KeycloakBuilder builder = KeycloakBuilder.builder()
                .serverUrl(properties.getServerUrl())
                .realm(properties.getAdminRealm())
                .clientId(properties.getClientId());

        if ("password".equals(properties.getGrantType())) {
            builder.grantType("password")
                    .username(properties.getUsername())
                    .password(properties.getPassword());
        } else {
            builder.grantType("client_credentials")
                    .clientSecret(properties.getClientSecret());
        }

        return builder.build();
    }

    @Bean
    public org.keycloak.admin.client.resource.RealmResource keycloakRealmResource(Keycloak keycloak, KeycloakAdminProperties properties) {
        return keycloak.realm(properties.getRealm());
    }
}
