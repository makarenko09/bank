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

--
