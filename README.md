# 🚀 PaatoM Bank_REST — Система Управления Банковскими Картами

[![Seed4j](https://img.shields.io/badge/SEED4J-Generator-4A90E2?style=for-the-badge)](https://seed4j.com/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.2-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Gradle](https://img.shields.io/badge/Gradle-9.3.0-02303A?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org/)
[![Docker](https://img.shields.io/badge/Docker-24.0-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=spring-security&logoColor=white)](https://spring.io/projects/spring-security)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18.2-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Keycloak](https://img.shields.io/badge/Keycloak-26.0.7-333?style=for-the-badge&logo=keycloak&logoColor=white)](https://www.keycloak.org/)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0-85EA2D?style=for-the-badge&logo=openapi-initiative&logoColor=black)](https://swagger.io/specification/)
[![Liquibase](https://img.shields.io/badge/Liquibase-291A28?style=for-the-badge&logo=liquibase&logoColor=white)](https://www.liquibase.com/)

---

## 📋 Описание проекта

Разработка **Системы Управления Банковскими Картами** для безопасного управления карточными счетами.

### Функциональность

- ✅ Создание и управление картами
- ✅ Просмотр карт (поиск + пагинация)
- ✅ Переводы между своими картами
- ✅ Блокировка/разблокировка карт
- ✅ Просмотр баланса
- ✅ Маскирование номеров карт (**** **** **** 1234)

### Роли пользователей

| Роль | Возможности |
|------|-------------|
| **ADMIN** | Создание, блокировка, активация, удаление карт<br>Управление пользователями<br>Просмотр всех карт в системе |
| **USER** | Просмотр своих карт<br>Запрос блокировки карты<br>Переводы между своими картами<br>Просмотр баланса |

---

## 📁 Структура проекта

```
myApp/
├── src/main/
│   ├── java/com/example/bankcards/
│   │   ├── transaction/          # Карты и клиенты
│   │   │   ├── domain/           # Бизнес-логика
│   │   │   └── infrastructure/   # Реализация
│   │   ├── shared/               # Общие компоненты
│   │   │   ├── authentication/   # Keycloak интеграция
│   │   │   └── error/            # Обработка ошибок
│   │   └── wire/                 # Конфигурация
│   ├── resources/
│   │   ├── config/               # Конфигурация приложения
│   │   ├── db/migration/         # Liquibase миграции
│   │   └── openapi.yml           # OpenAPI спецификация
│   └── docker/                   # Docker конфигурации
├── docker-compose.yml            # Основная Docker Compose
├── build.gradle.kts              # Gradle сборка
└── KEYCLOAK_INTEGRATION.md       # Документация Keycloak
```

---

## 🔧 Prerequisites

### Java 25 & gradle

```bash
# Установка SDKMAN
export SDKMAN_DIR="$HOME/.sdkman"
[[ -s "$SDKMAN_DIR/bin/sdkman-init.sh" ]] && source "$SDKMAN_DIR/bin/sdkman-init.sh"

# Установка Java 25 & Gradle 8.5
sdk env

```

### Docker

```bash
# Проверка установки
docker --version
docker compose version
```

---

## 🚀 Быстрый старт

### 1. Запуск инфраструктуры

```bash
# Запуск PostgreSQL и Keycloak
docker compose -f docker-compose.yml up -d

# Проверка статуса
docker compose ps

# Просмотр логов
docker compose logs -f
```

**Сервисы:**
- **PostgreSQL:** `localhost:5432` (bank/bank)
- **Keycloak:** [http://localhost:9081](http://localhost:9081) (admin/admin)
- **Приложение:** [http://localhost:8080](http://localhost:8080)

### 2. Запуск приложения

```bash
# Запуск через Gradle
gradle bootRun

# Или сборка и запуск JAR
gradle bootJar
java -jar build/libs/bankcards-0.0.1-SNAPSHOT.jar
```

### 3. Проверка работы

- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **Keycloak Admin:** [http://localhost:9081/admin/master/console](http://localhost:9081/admin/master/console)

---

## 📚 Документация

### API Документация

- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI Spec:** `src/main/resources/openapi.yml`

### Руководства

- [Keycloak Integration](KEYCLOAK_INTEGRATION.md) — полная документация по интеграции с Keycloak
- [Hexagonal Architecture](documentation/hexagonal-architecture.md)
- [Package Types](documentation/package-types.md)
- [PostgreSQL](documentation/postgresql.md)
- [CORS Configuration](documentation/cors-configuration.md)

---

## 🧪 Тестирование

### Запуск тестов

```bash
# Юнит-тесты
gradle test

# Интеграционные тесты
gradle integrationTest

# Все тесты
gradle check
```

### Покрытие тестами

Ключевая бизнес-логика покрыта тестами:
- Валидация данных карт
- Переводы между картами
- Блокировка/активация
- Проверка прав доступа

---

## 🔐 Безопасность

### Аутентификация

- **OAuth2/OIDC** через Keycloak
- **JWT токены** для API запросов
- **Роли:** `ROLE_ADMIN`, `ROLE_USER`

### Получение токена

```bash
# Получение токена
TOKEN=$(curl -X POST http://localhost:9081/realms/seed4j/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=web_app" \
  -d "username=admin" \
  -d "password=admin" | jq -r '.access_token')

# Использование токена
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/admin/clients
```

### Шифрование данных

- Номера карт зашифрованы
- Маскирование: `**** **** **** 1234`
- Пароли хешируются (PBKDF2-SHA256)

---

## 📊 База данных

### PostgreSQL

```yaml
image: postgres:18.2
database: bank
user: bank
password: bank
```

### Liquibase миграции

Миграции расположены в `src/main/resources/db/migration/`:

```xml
<!-- 0000000000_example.xml -->
<changeSet id="create_client_account" author="developer">
  <createTable tableName="client_account">
    <column name="user_id" type="UUID">
      <constraints primaryKey="true"/>
    </column>
    <column name="owner_name" type="VARCHAR(255)">
      <constraints nullable="false"/>
    </column>
  </createTable>
</changeSet>
```
> [!WARNING]
Не забудьте включить миграции в используемом файле профилирования для получения действующих DDL соответствующим сущностям проекта используя файл
``` ~/src/main/resources/config/liquibase/changelog/migration/start_development_db.sql```.
А также поправить настройки hibernate в вашем окружении используюго схему .YML:
``` spring:```
   ```  jpa:```
     ```    hibernate:```
       ```      ddl-auto: <option>```


---

## 🎯 API Endpoints

### Client Management (ADMIN)

| Метод | Endpoint | Описание |
|-------|----------|----------|
| POST | `/api/admin/clients` | Создать клиента |
| POST | `/api/admin/clients/sync` | Создать с синхронизацией Keycloak |
| GET | `/api/admin/clients` | Все клиенты |
| GET | `/api/admin/clients/{ownerName}` | Клиент по имени |
| DELETE | `/api/admin/clients/{clientId}` | Удалить клиента |

### Card Management (ADMIN)

| Метод | Endpoint | Описание |
|-------|----------|----------|
| GET | `/api/admin/cards` | Все карты |
| GET | `/api/admin/cards/{cardId}` | Карта по ID |
| PUT | `/api/admin/cards/{cardId}/block` | Заблокировать карту |
| PUT | `/api/admin/cards/{cardId}/activate` | Активировать карту |
| DELETE | `/api/admin/cards/{cardId}` | Удалить карту |

### User Card Management (USER)

| Метод | Endpoint | Описание |
|-------|----------|----------|
| GET | `/api/user/cards` | Мои карты |
| GET | `/api/user/cards/paginated` | Мои карты (пагинация) |
| GET | `/api/user/cards/{cardId}/balance` | Баланс карты |
| POST | `/api/user/cards/transfer` | Перевод между картами |
| PUT | `/api/user/cards/{cardId}/block` | Запрос блокировки |

### Keycloak Admin (ADMIN)

| Метод | Endpoint | Описание |
|-------|----------|----------|
| POST | `/api/admin/keycloak/users` | Создать пользователя |
| GET | `/api/admin/keycloak/users` | Все пользователи |
| DELETE | `/api/admin/keycloak/users/{userId}` | Удалить пользователя |
| POST | `/api/admin/keycloak/users/{userId}/roles/{roleName}` | Назначить роль |

---

## ❓ FAQ

**Q: Как получить доступ к Swagger UI?**

A: Откройте [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) после запуска приложения.

**Q: Как создать первого пользователя?**

A: Используйте Keycloak Admin Console ([http://localhost:9081](http://localhost:9081), admin/admin) или API:

```bash
curl -X POST http://localhost:9081/admin/realms/seed4j/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username": "user", "email": "user@example.com", "enabled": true}'
```

---

## 📝 License

REST_BANK — PaatoM Team

---

## 🌐 Futures steps on stage development. Section Supplements:

1. Документация/API Документация/Генерация документации & FAQ

* **Q: Как обновить OpenAPI спецификацию?**

A: При изменении API обновите `src/main/resources/openapi.yml` и выполните:

```bash
# Валидация спецификации
./gradlew validateOpenApi

# Генерация Markdown документации
./gradlew openApiGenerate

# Просмотр результатов
./gradlew viewOpenApiDocs
```

2. 🐳 Docker Compose: 
* **Запуск через Docker (образ via Jib)**

```bash
# 1. Сборка Docker образа через Jib
gradle jibDockerBuild
docker compose -f docker-compose-full.yml up -d
```

* **Управление сервисами**

```bash
# Проверка статуса
docker compose ps

# Остановка
docker compose down

# Остановка с очисткой данных
docker compose down -v

# Просмотр логов
docker compose logs -f bankapp
docker compose logs -f keycloak
docker compose logs -f postgresql
```

* **Структура Docker Compose файлов**

| Файл | Описание |
|------|----------|
| `docker-compose.yml` | Базовая конфигурация (PostgreSQL + Keycloak) |
| `docker-compose.full.yml` | Полное развёртывание (включает bank.yml) |
| `src/main/docker/bank.yml` | Конфигурация BankApp (образ via Jib) |
| `src/main/docker/postgresql.yml` | Конфигурация PostgreSQL |
| `src/main/docker/keycloak.yml` | Конфигурация Keycloak |

3. Customizing the Login Page for Keycloak

[Dev the custom theme](https://www.baeldung.com/keycloak-custom-login-page)
---

<!-- seed4j-needle-localEnvironment -->
<!-- seed4j-needle-startupCommand -->
<!-- seed4j-needle-documentation -->
