# Keycloak Integration Guide

## Обзор архитектуры

Данное руководство описывает интеграцию Keycloak с приложением PaatoM Bank_REST для управления аутентификацией и авторизацией пользователей.

### Архитектурная схема

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Запрос REST API                              │
│                              ↓                                       │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │              Spring Security (Filter Chain)                  │    │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │    │
│  │  │ OAuth2 Login    │  │ Bearer Token    │  │ Method      │ │    │
│  │  │ (OIDC Flow)     │  │ (Resource Server)│ │ Security    │ │    │
│  │  └────────┬────────┘  └────────┬────────┘  └──────┬──────┘ │    │
│  └───────────┼────────────────────┼──────────────────┼────────┘    │
│              │                    │                  │             │
│              ↓                    ↓                  ↓             │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │                    Keycloak Server                           │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │  │
│  │  │ Realm:      │  │ Users +     │  │ Roles:              │  │  │
│  │  │ seed4j      │  │ Credentials │  │ ROLE_ADMIN, ROLE_USER│  │  │
│  │  └─────────────┘  └─────────────┘  └─────────────────────┘  │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                              ↑                                     │
│              (Admin API / REST Client)                            │
└──────────────────────────────┼─────────────────────────────────────┘
                               │
                               ↓
┌─────────────────────────────────────────────────────────────────────┐
│                    Приложение (Spring Boot)                         │
│  ┌─────────────────────┐  ┌─────────────────────────────────────┐  │
│  │ KeycloakAdminClient │  │ AdministratorClientManagment        │  │
│  │ (управление через   │  │ (синхронизация PostgreSQL ↔         │  │
│  │  Admin REST API)    │  │  Keycloak по username)              │  │
│  └─────────────────────┘  └─────────────────────────────────────┘  │
│                              ↓                                      │
│              ┌───────────────────────────┐                         │
│              │   PostgreSQL Database     │                         │
│              │   (ClientAccount, Cards)  │                         │
│              └───────────────────────────┘                         │
└─────────────────────────────────────────────────────────────────────┘
```

## Порядок выполнения запроса

### 1. Аутентификация (OAuth2/OIDC)

```
Пользователь → /oauth2/authorization/oidc → Keycloak Login → 
→ Redirect с authorization_code → /login/oauth2/code/oidc → 
→ Обмен code на token → JWT в сессии
```

### 2. Авторизованный запрос к API

```
Запрос с JWT → Spring Security Filter Chain → 
→ BearerTokenAuthenticationFilter → 
→ OAuth2ResourceServer (проверка подписи JWT через JWKS) → 
→ AuthorizationFilter (@Secured, hasAuthority) → 
→ Контроллер
```

### 3. Что первичнее?

**Spring Security** — первый фильтр, обрабатывающий запрос.  
**Keycloak** — источник истины для:
- Выдачи JWT токенов (аутентификация)
- Хранения credentials пользователей
- Хранения ролей и групп

**Ваше приложение** — ресурс-сервер, который:
- Проверяет токены через Keycloak (JWKS endpoint)
- Управляет бизнес-данными в PostgreSQL

---

## Конфигурация

### Docker Keycloak (сервер)

**Файл:** `src/main/docker/keycloak.yml`

```yaml
services:
  keycloak:
    image: quay.io/keycloak/keycloak:26.0.7
    command: 'start-dev --import-realm'
    environment:
      - KEYCLOAK_ADMIN=admin          # Логин администратора
      - KEYCLOAK_ADMIN_PASSWORD=admin # Пароль администратора
      - KC_HTTP_PORT=9081
    ports:
      - '127.0.0.1:9081:9081'
```

**Важно:** Это development конфигурация. Для production:
- Используйте `start` вместо `start-dev`
- Настройте HTTPS
- Используйте внешнюю БД (PostgreSQL)

### Application Configuration (клиент)

**Файл:** `src/main/resources/config/application.yml`

```yaml
# OAuth2 Client (для аутентификации пользователей)
spring:
  security:
    oauth2:
      client:
        provider:
          oidc:
            issuer-uri: http://localhost:9081/realms/seed4j
        registration:
          oidc:
            client-id: web_app
            client-secret: web_app
            scope: openid,profile,email

# Keycloak Admin Client (для программного управления)
keycloak:
  admin:
    server-url: http://localhost:9081
    admin-realm: master          # Реалм для администрирования
    realm: seed4j                # Реалм для управления пользователями
    client-id: admin-cli         # Client для Admin API
    username: admin              # Admin username
    password: admin              # Admin password
    grant-type: password
```

### Про `admin-cli` — важный вопрос безопасности

**Вопрос:** Почему используется `admin-cli` и безопасно ли это?

**Ответ:** `admin-cli` — это **встроенный клиент** Keycloak, который **уже существует** по умолчанию. Его **не нужно настраивать** вручную!

#### Проверка в Keycloak Admin Console

1. Откройте http://localhost:9081/admin/master/console или http://localhost:9081/admin/seed4j/console
2. Логин: `admin` / `admin`
3. Перейдите в **Clients** → увидите `admin-cli`

Этот клиент:
- **Public client** (без secret)
- Использует **Direct Access Grants** (password grant type)
- Предназначен для CLI и скриптов

#### Архитектура безопасности

```
┌─────────────────────────────────────────────────────────────┐
│  Реалм: master (административный)                           │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ admin-cli (встроенный клиент)                        │   │
│  │ - Не требует client-secret                           │   │
│  │ - Требует username/password администратора           │   │
│  │ - Права: управление ВСЕМИ реалмами                   │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  Пользователь: admin                                        │
│  - Realm Roles: admin, create-realm                         │
│  - Автоматически управляет всеми реалмами                  │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│  Реалм: seed4j (ваш бизнес-реалм)                           │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ admin-cli (встроенный клиент)                        │   │
│  │ - Не требует client-secret                           │   │
│  │ - Требует username/password администратора           │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  Пользователь: admin                                        │
│  - Client Roles (realm-management): realm-admin             │
│  - Права: управление только реалмом seed4j                  │
│                                                             │
│  Пользователи: user, admin (для входа в приложение)         │
│  - Realm Roles: ROLE_USER, ROLE_ADMIN                       │
│  - Клиенты: web_app (для OAuth2 login)                      │
└─────────────────────────────────────────────────────────────┘
```

**Разделение:**
- `master/admin-cli` + `admin@master` — для **администрирования всех реалмов** (создание пользователей, управление реалмами)
- `seed4j/admin-cli` + `admin@seed4j` — для **администрирования только реалма seed4j** (создание пользователей в seed4j)

---

## Best Practice: Использование бизнес-реалма для Admin CLI

**Использование `admin-cli` в бизнес-реалме — это стандарт безопасности для production-сред.**

### Почему бизнес-реалм лучше master для CLI

| Аспект | **Master Realm** (не рекомендуется) | **Business Realm** (лучшая практика) |
|--------|-------------------------------------|--------------------------------------|
| **Гранулярность прав** | Любой администратор — «суперпользователь» со всеми правами | Права ограничены только одним реалмом |
| **Риск ошибок** | Высокий: можно случайно затронуть другие реалмы | Низкий: действия ограничены текущим реалмом |
| **Изоляция токенов** | Токен действителен для всех реалмов | Токен действителен только для одного реалма |
| **Безопасность CI/CD** | Сервисный аккаунт имеет доступ к системным настройкам | Сервисный аккаунт ограничен бизнес-реалмом |
| **Аудит** | Сложно отделить действия приложения от системных | Чёткое разграничение действий |

### Практическая разница в командах kcadm.sh

| Действие | Через Master (не реком.) | Через Бизнес-реалм (лучшая практика) |
|----------|--------------------------|--------------------------------------|
| **Логин** | `kcadm.sh config credentials --realm master --user admin ...` | `kcadm.sh config credentials --realm seed4j --user cli-admin ...` |
| **Создание пользователя** | `kcadm.sh create users -r seed4j -s username=user1` | `kcadm.sh create users -s username=user1` |
| **Контекст** | Требуется `-r target-realm` | Работает в контексте текущего реалма |

---

### Настройка доступа в бизнес-реалме (Production)

Для корректной работы `admin-cli` в бизнес-реалме **без использования master**:

#### Шаг 1: Создайте технического пользователя

1. Откройте **Seed4j Realm** → **Users** → **Create user**
   - Username: `cli-admin` (или `keycloak-service`)
   - Email: `cli-admin@yourdomain.com`
   - Enabled: `ON`

2. Вкладка **Credentials**:
   - Set password: `<strong-password>`
   - Temporary: `OFF`

#### Шаг 2: Назначьте роли realm-management

1. Перейдите во вкладку **Role mapping** → **Assign role**
2. Выберите **Filter by clients** → `realm-management`
3. Выберите роль **realm-admin** (полные права на реалм)
   - Альтернатива для минимальных прав: `manage-users`, `view-users`, `query-users`

#### Шаг 3: Используйте в конфигурации

```yaml
keycloak:
  admin:
    server-url: http://localhost:9081
    admin-realm: seed4j              # Бизнес-реалм для аутентификации
    realm: seed4j                    # Управляемый реалм
    client-id: admin-cli
    username: cli-admin              # Технический пользователь
    password: <password>
    grant-type: password
```

#### Шаг 4: Проверка через kcadm.sh

```bash
# Настройка credentials в бизнес-реалме
kcadm.sh config credentials \
  --server http://localhost:9081 \
  --realm seed4j \
  --user cli-admin \
  --password <password> \
  --client-id admin-cli

# Создание пользователя (не нужно указывать -r target-realm)
kcadm.sh create users -s username=newuser -s email=newuser@example.com

# Назначение роли
kcadm.sh add-roles --username newuser --rolename ROLE_USER
```

---

### Альтернатива: Service Account для CI/CD (Production)

Для автоматизации в production используйте **Service Account** вместо password grant:

#### Создание клиента с Service Account

1. **Seed4j Realm** → **Clients** → **Create**
   - Client ID: `bank-admin-api`
   - Client Protocol: `openid-connect`
   - Access Type: `Confidential`
   - Service Accounts Enabled: `ON`
   - Direct Access Grants Enabled: `OFF` (не требуется для client_credentials)

2. Вкладка **Service Account Roles**:
   - Assign Role → **realm-management** → `realm-admin` (или минимальные: `manage-users`, `view-users`)

3. Вкладка **Credentials**:
   - Скопируйте **Client Secret**

4. Обновите конфигурацию:

```yaml
keycloak:
  admin:
    server-url: http://localhost:9081
    admin-realm: seed4j
    realm: seed4j
    client-id: bank-admin-api
    client-secret: <secret-from-credentials>
    grant-type: client_credentials  # Без username/password
```

**Преимущества Service Account:**
- Отдельные credentials для приложения (не пароль пользователя)
- Можно отозвать доступ без смены пароля
- Чёткий аудит: действия от имени сервиса, не пользователя
- Рекомендуется для CI/CD и микросервисов

---

### Сравнение подходов

| Подход | Development | Production |
|--------|-------------|------------|
| **master/admin-cli + admin@master** | ✅ Просто, работает из коробки | ❌ Избыточные права |
| **seed4j/admin-cli + admin@seed4j** | ✅ Достаточно для тестов | ⚠️ Требует настройки ролей |
| **seed4j/admin-cli + cli-admin@seed4j** | ✅ Лучшая практика | ✅ Рекомендуется |
| **Service Account (bank-admin-api)** | ⚠️ Требует настройки | ✅ **Best Practice для CI/CD** |

---

### Как конфигурация подтягивается

1. **`@ConfigurationProperties(prefix = "keycloak.admin")`** — Spring Boot автоматически маппит YAML-свойства на Java-объект `KeycloakAdminProperties`

2. **`@EnableConfigurationProperties(KeycloakAdminProperties.class)`** — регистрирует бин properties в контексте

3. **`KeycloakAdminConfig.keycloakAdminClient()`** — создаёт `Keycloak` клиент, используя свойства

---

## Компоненты интеграции

### 1. KeycloakAdminConfig

**Файл:** `src/main/java/.../authentication/infrastructure/primary/KeycloakAdminConfig.java`

```java
@Configuration
@EnableConfigurationProperties(KeycloakAdminProperties.class)
public class KeycloakAdminConfig {
    
    @Bean
    public ObjectMapper keycloakObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Настройки согласно документации Keycloak:
        // https://www.keycloak.org/securing-apps/admin-client
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
    
    @Bean
    public Keycloak keycloakAdminClient(KeycloakAdminProperties properties) {
        return KeycloakBuilder.builder()
            .serverUrl(properties.getServerUrl())
            .realm(properties.getAdminRealm())
            .clientId(properties.getClientId())
            // ... grant type и credentials
            .build();
    }
}
```

**Назначение:**
- Создаёт бин `Keycloak` для Admin API вызовов
- Настраивает `ObjectMapper` для совместимости между версиями Keycloak

### 2. KeycloakAdminService

**Файл:** `src/main/java/.../authentication/infrastructure/primary/KeycloakAdminService.java`

```java
@Service
public class KeycloakAdminService {
    
    public String createUser(String username, String email, String password, boolean enabled) {
        // POST /admin/realms/{realm}/users
    }
    
    public void assignRole(String userId, String roleName) {
        // POST /admin/realms/{realm}/users/{id}/role-mappings/realm
    }
    
    public void deleteUser(String userId) {
        // DELETE /admin/realms/{realm}/users/{id}
    }
    
    // ... другие методы
}
```

**Методы:**
| Метод | Описание |
|-------|----------|
| `createUser()` | Создать пользователя в Keycloak |
| `getUserByUsername()` | Поиск по username |
| `assignRole()` | Назначить роль |
| `revokeRole()` | Отозвать роль |
| `deleteUser()` | Удалить пользователя |
| `enableUser()` / `disableUser()` | Активировать/заблокировать |
| `setPassword()` | Установить пароль |

### 3. KeycloakAdminController

**Файл:** `src/main/java/.../authentication/infrastructure/primary/KeycloakAdminController.java`

REST API для управления Keycloak через Admin Client.

**Endpoints:**

| Метод | Endpoint | Описание |
|-------|----------|----------|
| `POST` | `/api/admin/keycloak/users` | Создать пользователя |
| `GET` | `/api/admin/keycloak/users/{username}` | Получить по username |
| `GET` | `/api/admin/keycloak/users` | Все пользователи |
| `DELETE` | `/api/admin/keycloak/users/{userId}` | Удалить |
| `PUT` | `/api/admin/keycloak/users/{userId}/enable` | Активировать |
| `PUT` | `/api/admin/keycloak/users/{userId}/disable` | Заблокировать |
| `POST` | `/api/admin/keycloak/users/{userId}/roles/{roleName}` | Назначить роль |
| `GET` | `/api/admin/keycloak/roles` | Все роли реалма |

**Безопасность:** `@Secured("ROLE_ADMIN")` — требуется роль администратора.

### 4. AdministratorClientManagment + ClientManagementController

**Файлы:**
- `src/main/java/.../transaction/infrastructure/secondary/AdministratorClientManagment.java`
- `src/main/java/.../transaction/infrastructure/primary/ClientManagementController.java`

Сервис для управления клиентами с синхронизацией между PostgreSQL и Keycloak.

#### Методы синхронизации

```java
@Service
public class AdministratorClientManagment {
    
    /**
     * Создать клиента в PostgreSQL + синхронизировать с Keycloak
     */
    @Transactional
    public ClientAccount createClientAccountWithKeycloakSync(
        String ownerName, String email, String password) {
        // 1. Создаём в PostgreSQL
        ClientAccount account = new ClientAccount(ownerName);
        repository.save(account);
        
        // 2. Создаём в Keycloak (username = ownerName)
        keycloakAdminService.createUser(ownerName, email, password, true);
        
        // 3. Назначаем роль ROLE_USER
        keycloakAdminService.assignRole(userId, "ROLE_USER");
        
        return account;
    }
    
    /**
     * Синхронизировать существующего клиента с Keycloak
     */
    @Transactional
    public boolean syncClientWithKeycloak(
        String ownerName, String email, String password) {
        // Проверяем существование в PostgreSQL
        getClientAccount(ownerName);
        
        // Если нет в Keycloak — создаём
        if (!keycloakAdminService.userExists(ownerName)) {
            keycloakAdminService.createUser(ownerName, email, password, true);
            return true;
        }
        return false;
    }
    
    /**
     * Удалить из PostgreSQL и Keycloak
     */
    @Transactional
    public void deleteClientWithKeycloakSync(UUID clientId) {
        // 1. Получаем ownerName из PostgreSQL
        ClientAccount account = repository.findById(clientId).orElseThrow();
        
        // 2. Удаляем из PostgreSQL
        repository.deleteById(clientId);
        
        // 3. Удаляем из Keycloak (не критично если ошибка)
        keycloakAdminService.getUserByUsername(account.getOwnerName())
            .ifPresent(user -> keycloakAdminService.deleteUser(user.getId()));
    }
}
```

#### REST API для синхронизации

**ClientManagementController** предоставляет endpoints:

| Метод | Endpoint | Описание |
|-------|----------|----------|
| `POST` | `/api/admin/clients/sync` | Создать клиента в PostgreSQL + Keycloak |
| `POST` | `/api/admin/clients/{ownerName}/sync` | Синхронизировать существующего клиента |
| `POST` | `/api/admin/clients` | Создать только в PostgreSQL (без Keycloak) |
| `GET` | `/api/admin/clients` | Получить всех клиентов |
| `GET` | `/api/admin/clients/{ownerName}` | Получить клиента по имени |
| `DELETE` | `/api/admin/clients/{clientId}` | Удалить клиента (из PostgreSQL + Keycloak) |

**Пример запроса:**

```bash
# Создать клиента с синхронизацией Keycloak
curl -X POST http://localhost:8080/api/admin/clients/sync \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "ownerName": "john",
    "email": "john@example.com",
    "password": "secret123"
  }'

# Ответ:
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "ownerName": "john",
  "synced": true
}
```

## Стратегия синхронизации

### Связь между системами

```
PostgreSQL (ClientAccount)          Keycloak (User)
─────────────────────────          ─────────────────────
ownerName (String)      ←────→     username (String)
                                   email (String)
                                   credentials (password)
                                   roles (ROLE_USER, ROLE_ADMIN)
```

**Ключевой принцип:** `username` в Keycloak = `ownerName` в PostgreSQL

### Почему не дублируем ID?

- **Keycloak UUID** — внутренний идентификатор Keycloak (может измениться при миграции)
- **PostgreSQL UUID** — генерируется базой данных
- **username** — стабильный бизнес-ключ, понятный в обеих системах

### Поток создания пользователя

```
1. Запрос: POST /api/clients
   { "ownerName": "john", "email": "john@example.com" }

2. PostgreSQL: INSERT INTO client_account (owner_name, ...)
   → userId = 550e8400-e29b-41d4-a716-446655440000

3. Keycloak: POST /admin/realms/seed4j/users
   { "username": "john", "email": "john@example.com", ... }
   → keycloakId = "a1b2c3d4-..."

4. Keycloak: POST /admin/realms/seed4j/users/{id}/role-mappings
   [ { "name": "ROLE_USER" } ]

5. Результат: пользователь может войти через Keycloak
   и получить доступ к данным в PostgreSQL
```

---

## Версии и совместимость

### Используемые версии

| Компонент | Версия |
|-----------|--------|
| Keycloak Server | 26.0.7 |
| keycloak-admin-client | 26.0.7 |
| Spring Boot | 4.0.2 |
| Java | 25 |

### Настройки ObjectMapper

Согласно [официальной документации Keycloak](https://www.keycloak.org/securing-apps/admin-client):

```java
objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
```

**Зачем:**
- `NON_NULL` — не сериализовать `null` значения (требование API Keycloak)
- `FAIL_ON_UNKNOWN_PROPERTIES = false` — игнорировать неизвестные поля (для совместимости между версиями)

---

## REST API Keycloak Admin

### Основные endpoints

Документация: https://www.keycloak.org/docs-api/26.0.7/rest-api/

| Описание | HTTP | Path |
|----------|------|------|
| Создать пользователя | POST | `/admin/realms/{realm}/users` |
| Получить всех пользователей | GET | `/admin/realms/{realm}/users` |
| Получить пользователя по ID | GET | `/admin/realms/{realm}/users/{id}` |
| Поиск по username | GET | `/admin/realms/{realm}/users?username={name}` |
| Обновить пользователя | PUT | `/admin/realms/{realm}/users/{id}` |
| Удалить пользователя | DELETE | `/admin/realms/{realm}/users/{id}` |
| Назначить роль | POST | `/admin/realms/{realm}/users/{id}/role-mappings/realm` |
| Получить роли | GET | `/admin/realms/{realm}/users/{id}/role-mappings/realm` |
| Создать роль | POST | `/admin/realms/{realm}/roles` |
| Получить все роли | GET | `/admin/realms/{realm}/roles` |

### Пример запроса через curl

#### Development (admin@seed4j)

```bash
# Получить токен администратора (seed4j realm)
TOKEN=$(curl -X POST http://localhost:9081/realms/seed4j/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=admin" | jq -r '.access_token')

# Создать пользователя
curl -X POST http://localhost:9081/admin/realms/seed4j/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "email": "user@example.com",
    "enabled": true,
    "credentials": [{
      "type": "password",
      "value": "password123",
      "temporary": false
    }]
  }'
```

#### Production (Service Account)

```bash
# Получить токен сервисного аккаунта (client_credentials)
TOKEN=$(curl -X POST http://localhost:9081/realms/seed4j/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=bank-admin-api" \
  -d "client_secret=<client-secret>" | jq -r '.access_token')

# Создать пользователя
curl -X POST http://localhost:9081/admin/realms/seed4j/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "email": "user@example.com",
    "enabled": true
  }'
```

---

## Безопасность

### Уровни защиты

1. **Transport Level** — HTTPS (обязательно для production)
2. **Authentication** — OAuth2/OIDC через Keycloak
3. **Authorization** — Spring Security (`@Secured`, `@PreAuthorize`)
4. **API Level** — `/api/admin/**` требует `ROLE_ADMIN`

### Конфиденциальные данные

**Никогда не логируйте:**
- Пароли пользователей
- Client Secret
- Access Token (полностью)

**Хранение секретов:**
- Development: `application.yml` (не коммитить в git!)
- Production: Environment variables, Vault, AWS Secrets Manager

---

## Troubleshooting

### Ошибка: "UnrecognizedPropertyException: multivalued"

**Причина:** Несовпадение версий Keycloak Server и Admin Client.

**Решение:** Обновить версию в `gradle/libs.versions.toml`:
```toml
[versions]
keycloak = "26.0.7"  # Должна совпадать с версией сервера
```

### Ошибка: "username required"

**Причина:** Используется `grant_type=password` без указания username/password.

**Решение:** Проверить конфигурацию:
```yaml
keycloak:
  admin:
    grant-type: password  # или client_credentials
    username: admin       # требуется для password grant
    password: admin
```

### Ошибка: 401 Unauthorized при вызове Admin API

**Причина:** Неверные credentials или пользователь не имеет прав.

**Решение:**
1. Проверить `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` в Docker
2. Убедиться, что клиент `admin-cli` существует в реалме (master или seed4j)
3. Проверить права пользователя:
   - Для `master/admin-cli`: пользователь `admin@master` имеет автоматические права на все реалмы
   - Для `seed4j/admin-cli`: пользователь должен иметь роль `realm-admin` в клиенте `realm-management`
4. Проверить `admin-realm` в конфигурации (должен совпадать с реалмом пользователя)

### Ошибка: 401 Unauthorized для MVP (локальная разработка)

**Причина:** Устаревший JWT-токен в кеше браузера или приложения. Токен содержит старые роли или данные пользователя.

**Контекст для MVP:**
В минимальной версии приложения (MVP) часто используется **упрощённая аутентификация** без полноценной синхронизации ролей между Keycloak и приложением.

**Симптомы:**
- Пользователь успешно вошёл через Keycloak
- В Keycloak назначены новые роли
- Приложение возвращает `401 Unauthorized` или `403 Forbidden`
- В токене старые роли (не обновлены после изменений в Keycloak)

**Решение:**

#### 1. Очистить куки и localStorage браузера

```bash
# Chrome DevTools → Application → Storage
# Нажмите "Clear site data" или:
# - Cookies для localhost:8080
# - Local Storage → ключи OAuth2
# - Session Storage
```

Или через консоль браузера (F12):
```javascript
// Очистить cookies
document.cookie.split(";").forEach(c => document.cookie = c.replace(/^ +/, "").replace(/=.*/, "=;expires=" + new Date().toUTCString() + ";path=/"));

// Очистить localStorage
localStorage.clear();

// Очистить sessionStorage
sessionStorage.clear();
```

#### 2. Принудительный logout из Keycloak

```bash
# URL для logout (замените параметры)
http://localhost:9081/realms/seed4j/protocol/openid-connect/logout?post_logout_redirect_uri=http://localhost:8080
```

#### 3. Проверить содержимое токена

Расшифруйте JWT токен на https://jwt.io или через консоль:
```javascript
// Вставьте ваш access_token
const token = "eyJhbGc...";
const payload = JSON.parse(atob(token.split('.')[1]));
console.log('Roles:', payload.realm_access?.roles);
console.log('Client Roles:', payload.resource_access);
```

#### 4. Для MVP: отключить кеширование токенов (development)

```yaml
spring:
  security:
    oauth2:
      client:
        provider:
          oidc:
            issuer-uri: http://localhost:9081/realms/seed4j
            # Принудительное обновление токена
            user-info-authentication-uri: http://localhost:9081/realms/seed4j/protocol/openid-connect/userinfo
```

#### 5. Перезапустить сессию приложения

```bash
# Остановить приложение
# Очистить куки браузера
# Запустить заново
./gradlew bootRun
```

---

### Ошибка: Роли не обновляются после изменений в Keycloak

**Причина:** Keycloak выдаёт токен при логине, но приложение не запрашивает новый токен после изменения ролей.

**Решение для MVP:**
1. **Logout → Login** — полный перелогин для получения нового токена
2. **Настроить refresh token** (если поддерживается)
3. **Для production:** использовать short-lived access tokens (5-10 мин) + refresh tokens

---

### Ошибка: 403 Forbidden при вызове Admin API

**Причина:** Пользователь аутентифицирован, но не имеет достаточных прав.

**Решение:**
1. Проверить роли пользователя в Admin Console:
   - **Users** → выбрать пользователя → **Role mapping**
   - Убедиться, что назначена роль `realm-admin` (или `manage-users`) из клиента `realm-management`
2. Для Service Account:
   - **Clients** → выбрать клиент → **Service Account Roles**
   - Назначить роли из `realm-management`

### Ошибка: "Invalid client credentials"

**Причина:** Неверный `client_id` или `client-secret` (для Service Account).

**Решение:**
1. Проверить `client-id` в конфигурации
2. Для Service Account: обновить `client-secret` из вкладки **Credentials** клиента
3. Убедиться, что клиент имеет **Access Type: Confidential**

---

## Миграция и обновления

### Обновление версии Keycloak

1. Обновить `keycloak` в `libs.versions.toml`
2. Обновить `image` в `keycloak.yml`
3. Проверить [migration guide](https://www.keycloak.org/docs/latest/upgrading/)
4. Протестировать на staging

### Бэкап реалма

```bash
# Экспорт реалма
curl -X GET http://localhost:9081/admin/realms/seed4j \
  -H "Authorization: Bearer $TOKEN" > seed4j-realm.json
```

---

## Сборка и развёртывание приложения

### 1. Сборка executable JAR (Spring Boot)

**Базовая сборка:**
```bash
./gradlew bootJar
```

**Результат:** `build/libs/bankcards-0.0.1-SNAPSHOT.jar`

**Запуск:**
```bash
java -jar build/libs/bankcards-0.0.1-SNAPSHOT.jar
```

---

### 2. Сборка через bootBuildImage (Docker image)

**Плагин Spring Boot** автоматически создаёт Docker-образ:

```bash
./gradlew bootBuildImage
```

**Параметры образа:**
- Имя: `docker.io/library/bankcards:0.0.1-SNAPSHOT`
- Основан на Paketo Buildpacks
- Автоматически определяет Java версию

**Кастомизация имени образа:**
```bash
./gradlew bootBuildImage --imageName=myregistry/bankcards:latest
```

**Запуск контейнера:**
```bash
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  docker.io/library/bankcards:0.0.1-SNAPSHOT
```

---

### 3. Сборка через Jib (Google Jib Plugin)

**Добавьте плагин в `build.gradle.kts`:**

```kotlin
plugins {
  java
  alias(libs.plugins.spring.boot)
  id("com.google.cloud.tools.jib") version "3.4.0"  // ← Добавить
}

// Конфигурация Jib
jib {
  from {
    image = "eclipse-temurin:25-jre"  // Базовый образ с JRE 25
  }
  to {
    image = "myregistry/bankcards:latest"
    // auth {
    //   username = System.getenv("DOCKER_USERNAME")
    //   password = System.getenv("DOCKER_PASSWORD")
    // }
  }
  container {
    mainClass = "com.example.bankcards.BankApp"
    ports = listOf("8080")
    environment = mapOf(
      "SPRING_PROFILES_ACTIVE" to "prod",
      "TZ" to "Europe/Moscow"
    )
    creationTime = "USE_CURRENT_TIMESTAMP"
  }
}
```

**Команды Jib:**

```bash
# Сборка в локальный Docker Daemon
./gradlew jibDockerBuild

# Сборка и отправка в registry
./gradlew jib

# Сборка с тегом
./gradlew jib --image=myregistry/bankcards:1.0.0
```

**Запуск экземпляра приложения:**
```bash
docker run -d \
  --name bank-app \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/bank \
  -e SPRING_DATASOURCE_USERNAME=bank \
  -e SPRING_DATASOURCE_PASSWORD=bank \
  myregistry/bankcards:latest
```

---

### 4. Кастомная задача bootJarOne (опционально)

Для создания нескольких JAR с разными конфигурациями:

```kotlin
import org.springframework.boot.gradle.tasks.bundling.BootJar

val MAIN_CLASS = "com.example.bankcards.BankApp"
val JAVA_VER = JavaVersion.VERSION_25
val RUNTIME_CP = sourceSets.main.get().runtimeClasspath

tasks.register<BootJar>("bootJarOne") {
  group = "build"
  description = "Build mono executable JARs"
  dependsOn("bootJar")
  
  archiveClassifier.set("mono")
  
  from {
    RUNTIME_CP.filter { it.name.endsWith(".jar") }
  }
}
```

**Запуск:**
```bash
./gradlew bootJarOne
```

---

### 5. Сравнение подходов

| Метод | Скорость | Кэш слоёв | Production | Примечание |
|-------|----------|-----------|------------|------------|
| **bootJar** | ⚡ Быстро | ❌ Нет | ⚠️ Ручная упаковка | Простая разработка |
| **bootBuildImage** | 🐢 Медленно | ✅ Да | ✅ Рекомендуется | Paketo Buildpacks |
| **Jib** | ⚡ Быстро | ✅ Да | ✅ **Best Practice** | Контроль базового образа |

**Рекомендация для MVP/Production:**
- **Разработка:** `bootJar` или `jibDockerBuild`
- **CI/CD:** `jib` (быстрее, детерминировано)
- **Production:** `jib` с кастомным базовым образом (Alpine/Distroless)

---

## ✅ Развертывание и тестирование

### Docker Compose для dev-среды

#### 1. Структура файлов

```
myApp/
├── docker-compose.yml              # Основной файл (include)
├── src/main/docker/
│   ├── postgresql.yml              # PostgreSQL конфигурация
│   ├── keycloak.yml                # Keycloak конфигурация
│   └── keycloak-realm-config/      # Конфигурация реалма seed4j
└── build/
    └── libs/
        └── bankcards-0.0.1-SNAPSHOT.jar
```

#### 2. Запуск инфраструктуры (PostgreSQL + Keycloak)

```bash
# Запуск только инфраструктуры (без приложения)
docker compose up -d postgresql keycloak

# Проверка статуса
docker compose ps

# Просмотр логов Keycloak
docker compose logs -f keycloak

# Просмотр логов PostgreSQL
docker compose logs -f postgresql
```

**Ожидаемый результат:**
```
NAME                STATUS                   PORTS
bank_rest-keycloak  Up (healthy)             127.0.0.1:9081->9081/tcp
bank_rest-postgresql Up                      127.0.0.1:5432->5432/tcp
```

#### 3. Проверка доступности сервисов

```bash
# Keycloak Health Check
curl http://localhost:9081/health/live

# Ответ: {"status":"UP"}

# Keycloak Admin Console
# Откройте в браузере: http://localhost:9081/admin/master/console
# Логин: admin / admin

# PostgreSQL подключение
docker compose exec postgresql psql -U bank -d bank -c "SELECT version();"
```

---

### Полное тестирование с OCI Image

#### Шаг 1: Сборка Docker-образа приложения

```bash
# Вариант A: Jib (рекомендуется)
./gradlew jibDockerBuild --image=bankcards:dev

# Вариант B: bootBuildImage
./gradlew bootBuildImage --imageName=bankcards:dev
```

**Проверка образа:**
```bash
docker images | grep bankcards

# Ответ:
# bankcards    dev    1.2GB    2 minutes ago
```

#### Шаг 2: Создание полной Docker Compose конфигурации

Создайте файл `docker-compose.full.yml` для полного развёртывания:

```yaml
name: bank_rest_full

services:
  # PostgreSQL
  postgresql:
    image: postgres:18.2
    environment:
      - POSTGRES_USER=bank
      - POSTGRES_PASSWORD=bank
      - POSTGRES_DB=bank
    ports:
      - '127.0.0.1:5432:5432'
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ['CMD-SHELL', 'pg_isready -U bank']
      interval: 5s
      timeout: 5s
      retries: 10

  # Keycloak
  keycloak:
    image: quay.io/keycloak/keycloak:26.0.7
    command: 'start-dev --import-realm'
    environment:
      - KC_DB=dev-file
      - KEYCLOAK_ADMIN=admin
      - KEYCLOAK_ADMIN_PASSWORD=admin
      - KC_HTTP_PORT=9081
      - KC_HEALTH_ENABLED=true
    ports:
      - '127.0.0.1:9081:9081'
    volumes:
      - ./src/main/docker/keycloak-realm-config:/opt/keycloak/data/import
    healthcheck:
      test: ['CMD-SHELL', 'curl -f http://localhost:9081/health/live']
      interval: 10s
      timeout: 5s
      retries: 30
    depends_on:
      postgresql:
        condition: service_healthy

  # Приложение BankCards
  bank-app:
    image: bankcards:dev
    ports:
      - '127.0.0.1:8080:8080'
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgresql:5432/bank
      - SPRING_DATASOURCE_USERNAME=bank
      - SPRING_DATASOURCE_PASSWORD=bank
      - SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_OIDC_ISSUER_URI=http://keycloak:9081/realms/seed4j
      - KEYCLOAK_ADMIN_SERVER_URL=http://keycloak:9081
      - KEYCLOAK_ADMIN_ADMIN_REALM=seed4j
      - KEYCLOAK_ADMIN_REALM=seed4j
      - KEYCLOAK_ADMIN_CLIENT_ID=admin-cli
      - KEYCLOAK_ADMIN_USERNAME=admin
      - KEYCLOAK_ADMIN_PASSWORD=admin
      - KEYCLOAK_ADMIN_GRANT_TYPE=password
    healthcheck:
      test: ['CMD-SHELL', 'curl -f http://localhost:8080/actuator/health']
      interval: 15s
      timeout: 10s
      retries: 10
    depends_on:
      postgresql:
        condition: service_healthy
      keycloak:
        condition: service_healthy

volumes:
  postgres_data:
```

#### Шаг 3: Запуск полного стека

```bash
# Запуск всех сервисов
docker compose -f docker-compose.full.yml up -d

# Проверка статуса
docker compose -f docker-compose.full.yml ps

# Ожидаемый результат:
# NAME                     STATUS
# bank_rest_full-postgresql   Up (healthy)
# bank_rest_full-keycloak     Up (healthy)
# bank_rest_full-bank-app     Up (healthy)
```

#### Шаг 4: Тестирование интеграции

##### 4.1. Проверка здоровья приложения

```bash
# Actuator Health Endpoint
curl http://localhost:8080/actuator/health

# Ответ:
# {"status":"UP","components":{...}}
```

##### 4.2. Получение токена доступа

```bash
# Токен для Admin API (seed4j realm)
ACCESS_TOKEN=$(curl -X POST http://localhost:9081/realms/seed4j/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=admin" | jq -r '.access_token')

echo "Token: $ACCESS_TOKEN"
```

##### 4.3. Создание пользователя через Admin API

```bash
# Создание пользователя
curl -X POST http://localhost:9081/admin/realms/seed4j/users \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "testuser@example.com",
    "enabled": true,
    "credentials": [{
      "type": "password",
      "value": "TestPass123!",
      "temporary": false
    }]
  }'

# Назначение роли ROLE_USER
USER_ID=$(curl -X GET "http://localhost:9081/admin/realms/seed4j/users?username=testuser" \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq -r '.[0].id')

curl -X POST "http://localhost:9081/admin/realms/seed4j/users/$USER_ID/role-mappings/realm" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '[{"name": "ROLE_USER"}]'
```

##### 4.4. Тестирование OAuth2 Login

```bash
# 1. Получение токена через OAuth2 flow (имитация)
OAUTH_TOKEN=$(curl -X POST http://localhost:9081/realms/seed4j/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=web_app" \
  -d "client_secret=web_app" \
  -d "username=testuser" \
  -d "password=TestPass123!" | jq -r '.access_token')

# 2. Запрос к защищённому API
curl -X GET http://localhost:8080/api/account \
  -H "Authorization: Bearer $OAUTH_TOKEN" \
  -H "Content-Type: application/json"
```

##### 4.5. Тестирование синхронизации PostgreSQL ↔ Keycloak

```bash
# Создание клиента через приложение (синхронизация с Keycloak)
APP_ADMIN_TOKEN=$(curl -X POST http://localhost:9081/realms/seed4j/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=admin" | jq -r '.access_token')

curl -X POST http://localhost:8080/api/admin/clients/sync \
  -H "Authorization: Bearer $APP_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "ownerName": "john_doe",
    "email": "john@example.com",
    "password": "SecurePass123!"
  }'

# Проверка в PostgreSQL
docker compose -f docker-compose.full.yml exec postgresql \
  psql -U bank -d bank -c "SELECT * FROM client_account WHERE owner_name = 'john_doe';"

# Проверка в Keycloak
curl -X GET "http://localhost:9081/admin/realms/seed4j/users?username=john_doe" \
  -H "Authorization: Bearer $APP_ADMIN_TOKEN"
```

#### Шаг 5: Проверка логов

```bash
# Логи приложения
docker compose -f docker-compose.full.yml logs -f bank-app

# Искать в логах:
# - "Application 'BankApp' is running!"
# - Подключение к PostgreSQL
# - Подключение к Keycloak
# - OAuth2 конфигурация
```

#### Шаг 6: Нагрузочное тестирование (опционально)

```bash
# Установка Apache Bench (если нет)
# sudo apt-get install apache2-utils

# Тестирование 1000 запросов, 10 параллельных
ab -n 1000 -c 10 \
  -H "Authorization: Bearer $OAUTH_TOKEN" \
  http://localhost:8080/api/account

# Ожидаемый результат:
# Complete requests:      1000
# Failed requests:        0
# Requests per second:    XXX.XX [#/sec]
```

#### Шаг 7: Остановка и очистка

```bash
# Остановка всех сервисов
docker compose -f docker-compose.full.yml down

# Остановка с удалением volumes (полная очистка)
docker compose -f docker-compose.full.yml down -v

# Удаление образа
docker rmi bankcards:dev
```

---

### Тест-кейсы для проверки

| № | Тест | Ожидаемый результат |
|---|------|---------------------|
| 1 | `GET /actuator/health` | `{"status":"UP"}` |
| 2 | Получение токена (admin) | `access_token` в ответе |
| 3 | Создание пользователя в Keycloak | HTTP 201 Created |
| 4 | Назначение роли пользователю | HTTP 204 No Content |
| 5 | OAuth2 Login (testuser) | Успешная аутентификация |
| 6 | Запрос к `/api/account` с токеном | HTTP 200 OK |
| 7 | Синхронизация client + Keycloak | Запись в PostgreSQL + пользователь в Keycloak |
| 8 | Logout и очистка сессии | Перенаправление на Keycloak logout |

---

### Troubleshooting для Docker Compose

#### Ошибка: "Container is unhealthy"

```bash
# Проверка логов
docker compose -f docker-compose.full.yml logs keycloak

# Перезапуск сервиса
docker compose -f docker-compose.full.yml restart keycloak
```

#### Ошибка: "Connection refused to PostgreSQL"

```bash
# Проверка доступности БД
docker compose -f docker-compose.full.yml exec postgresql pg_isready -U bank

# Проверка переменных окружения приложения
docker compose -f docker-compose.full.yml exec bank-app env | grep DATASOURCE
```

#### Ошибка: "401 Unauthorized от Keycloak"

```bash
# Проверка времени на сервере (рассинхронизация времени)
docker compose -f docker-compose.full.yml exec keycloak date

# Пересоздание токена
# (см. Шаг 4.2)
```

---


## Ссылки

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Keycloak Admin REST API](https://www.keycloak.org/docs-api/26.0.7/rest-api/)
- [Keycloak Admin Client](https://www.keycloak.org/securing-apps/admin-client)
- [Keycloak Server Admin Guide](https://www.keycloak.org/docs/latest/server_admin/)
- [Keycloak Client Scopes](https://www.keycloak.org/docs/latest/server_admin/#_client_scopes)
- [Spring Security OAuth2](https://docs.spring.io/spring-security/reference/servlet/oauth2/)
- [Spring Boot OAuth2 Properties](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#web.security.oauth2)
- [Spring Boot Gradle Plugin](https://docs.spring.io/spring-boot/gradle-plugin/reference/current/)
- [Google Jib Plugin](https://github.com/GoogleContainerTools/jib/tree/master/jib-gradle-plugin)
- [Paketo Buildpacks](https://paketo.io/)
- [Docker Compose Specification](https://docs.docker.com/compose/compose-file/)
- [Testcontainers Documentation](https://www.testcontainers.org/)

---

## 📚 OpenAPI Документация

### Обзор

Проект использует **OpenAPI 3.0** спецификацию для документирования REST API. Спецификация находится в файле `src/main/resources/openapi.yml`.

### Структура документации

```
src/main/resources/
├── openapi.yml              # Основная OpenAPI спецификация
└── config/
    └── application.yml      # Конфигурация Springdoc
```

### Генерация документации

#### Доступные Gradle задачи

| Задача | Описание |
|--------|----------|
| `validateOpenApi` | Валидация OpenAPI спецификации |
| `openApiGenerate` | Генерация Markdown документации |
| `generateOpenApiHtml` | Генерация HTML документации |
| `copyOpenApiSpec` | Копирование спецификации в build |
| `viewOpenApiDocs` | Просмотр сгенерированной документации |

#### Примеры использования

```bash
# Валидация спецификации
./gradlew validateOpenApi

# Генерация Markdown документации
./gradlew openApiGenerate

# Генерация HTML (требует дополнительной настройки)
./gradlew generateOpenApiHtml

# Копирование спецификации в build
./gradlew copyOpenApiSpec

# Просмотр документации
./gradlew viewOpenApiDocs
```

#### Результат генерации

После выполнения `./gradlew openApiGenerate`:

```
build/
└── docs/
    └── openapi/
        ├── README.md          # Основная документация
        ├── api/               # API endpoints
        ├── models/            # Модели данных
        └── authentication/    # Аутентификация
```

### Swagger UI (Runtime)

Приложение предоставляет **Swagger UI** во время выполнения:

- **URL:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs
- **OpenAPI YAML:** http://localhost:8080/v3/api-docs.yaml

#### Настройка Swagger UI

```yaml
# application.yml
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    operationsSorter: alpha
    tagsSorter: alpha
    tryItOutEnabled: true
  api-docs:
    path: /v3/api-docs
    enabled: true
```

### Редактирование спецификации

#### Добавление нового endpoint

1. Откройте `src/main/resources/openapi.yml`
2. Добавьте путь в секцию `paths`:

```yaml
/api/new-endpoint:
  get:
    tags:
      - Your Tag
    summary: Описание endpoint
    operationId: yourOperationId
    security:
      - bearerAuth: []
    responses:
      '200':
        description: Успешный ответ
```

3. Добавьте модель в `components.schemas` (если нужно):

```yaml
YourModel:
  type: object
  properties:
    id:
      type: string
      format: uuid
    name:
      type: string
```

4. Запустите валидацию: `./gradlew validateOpenApi`

#### Добавление тега

```yaml
tags:
  - name: Your Tag Name
    description: Описание группы API
```

### Интеграция с CI/CD

#### GitHub Actions пример

```yaml
name: OpenAPI Validation

on: [push, pull_request]

jobs:
  validate-openapi:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
      
      - name: Validate OpenAPI spec
        run: ./gradlew validateOpenApi
      
      - name: Generate documentation
        run: ./gradlew openApiGenerate
      
      - name: Upload documentation
        uses: actions/upload-artifact@v4
        with:
          name: openapi-docs
          path: build/docs/openapi/
```

### Best Practices

#### ✅ Делайте

- Поддерживайте спецификацию в актуальном состоянии
- Используйте `$ref` для переиспользования схем
- Документируйте все коды ответов
- Добавляйте примеры (`example`) для полей
- Используйте теги для группировки endpoints

#### ❌ Не делайте

- Не дублируйте схемы
- Не оставляйте endpoint без описания
- Не забывайте про security requirements
- Не игнорируйте ошибки валидации

### Инструменты

#### Редакторы

- [Swagger Editor](https://editor.swagger.io/) — онлайн редактор
- [Stoplight Studio](https://stoplight.io/studio) — десктопное приложение
- [VS Code Extension](https://marketplace.visualstudio.com/items?itemName=42Crunch.vscode-openapi) — плагин для VS Code

#### Валидация

```bash
# Через Spectral (CLI)
npm install -g @stoplight/spectral
spectral lint src/main/resources/openapi.yml

# Через Swagger CLI
npm install -g swagger-cli
swagger-cli validate src/main/resources/openapi.yml
```

#### Генерация клиентов

```bash
# Генерация Java клиента
openapi-generator generate -i src/main/resources/openapi.yml \
  -g java \
  -o generated-client

# Генерация TypeScript клиента
openapi-generator generate -i src/main/resources/openapi.yml \
  -g typescript-axios \
  -o generated-client-ts
```

### Версионирование

Формат версии: `MAJOR.MINOR.PATCH`

- **MAJOR** — несовместимые изменения API
- **MINOR** — новые возможности (обратно совместимые)
- **PATCH** — исправления ошибок

```yaml
info:
  version: 1.0.0  # Обновляйте при изменениях API
```

### Ссылки

- [OpenAPI Specification](https://swagger.io/specification/)
- [OpenAPI Generator](https://openapi-generator.tech/)
- [Springdoc OpenAPI](https://springdoc.org/)
- [Swagger UI](https://swagger.io/tools/swagger-ui/)
- [Stoplight Spectral](https://meta.stoplight.io/docs/spectral/)
