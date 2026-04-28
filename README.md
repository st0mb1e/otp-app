# OTP Service

Выполнил: Ярцев Владислав

Сервис генерации и валидации одноразовых паролей (One-Time Password) с
несколькими каналами доставки.

> **Конфигурация проекта.** Все настройки приложения (подключение к БД,
> параметры JWT, данные каналов доставки, значения по умолчанию для OTP)
> хранятся в файле [`src/main/resources/application.properties`](src/main/resources/application.properties).
> Каждое значение по умолчанию можно переопределить через переменную
> окружения или system property (`-D...`) - имена плейсхолдеров вида
> `${ENV_VAR:default}` указаны прямо в этом файле.

## Стек

| Слой | Технология |
| --- | --- |
| Язык / runtime | Java 17 |
| Сборка | Gradle (wrapper) |
| Веб-слой | Spring Boot 4 (`spring-boot-starter-web`, содержит `spring-mvc`) |
| СУБД | PostgreSQL 17 |
| Email | Jakarta Mail / Angus Mail 2.0.5 (SMTP) |
| Telegram | `telegrambots-longpolling` + `telegrambots-client` 9.5.0 |
| SMS | jsmpp 3.0.1 (SMPP, эмулятор SMPPsim) |

## Структура проекта

```
src/main/java/ru/yartsev_vladislav/otp_app
├── OtpAppApplication.java         # входная точка, содержит main-метод
├── config/                        # конфигурация
├── controller/                    # REST-контроллеры + GlobalExceptionHandler
├── dto/                           # DTO для запросов и ответов
├── domain/                        # JPA-сущности (User, OtpCode, OtpConfig) и энамы
├── exception/                     # бизнес-исключения (NotFound, Conflict, ...)
├── repository/                    # JPA-репозитории (слой DAO согласно ТЗ)
├── security/                      # JWT, фильтр, интеграция со Spring MVC
└── service/
    ├── AuthService / OtpService / OtpConfigService / UserAdminService - интерфейсы
    ├── impl/                      # реализации сервисов
    └── notification/              # реализации каналов доставки (FILE / EMAIL / SMS / TELEGRAM)
```

Архитектура классическая трёхслойная: контроллеры -> сервисы -> репозитории
(`JpaRepository` поверх JDBC-драйвера PostgreSQL). Подробное описание
каждого пакета приведено ниже в разделе "Документация по модулям".

### Таблицы БД (создаются автоматически через `ddl-auto=update`)

- **`users`** - `id`, `login` (unique), `password_hash` (BCrypt), `role` (`ADMIN` / `USER`).
- **`otp_config`** - таблица-синглтон (`id = 1`) с двумя полями: `code_length`,
  `ttl_seconds`. Запись всегда одна: код обращается к ней по фиксированному
  `SINGLETON_ID`.
- **`otp_codes`** - `id`, `user_id`, `operation_id`, `code`, `status`
  (`ACTIVE` / `EXPIRED` / `USED`), `created_at`, `expires_at`. Индексы по
  `user_id`, `(status, expires_at)`, `operation_id`.

## Документация по модулям

Ниже перечислены все пакеты приложения, их состав и точки соприкосновения
с другими пакетами.

### Корневой пакет (`ru.yartsev_vladislav.otp_app`)

- `OtpAppApplication` - точка входа с методом `main`. На класс навешены
  `@SpringBootApplication`, `@ConfigurationPropertiesScan` (чтобы
  Spring подхватил `OtpProperties` и `JwtProperties`) и
  `@EnableScheduling` (для шедулера пометки просроченных кодов).

### Пакет `config`

- `OtpProperties` - record для префикса `app.otp` с полями
  `fileOutputDir`, `defaultCodeLength` (4..10) и
  `defaultTtlSeconds` (>= 10). Читается `OtpConfigServiceImpl` при
  первом старте (для заполнения таблицы `otp_config`) и
  `FileOtpNotificationSender` (для выбора каталога вывода).

### Пакет `controller`

Контроллеры являются единственным HTTP-входом в приложение. Поля DTO
валидируются через `@Valid` и Bean Validation.

- `AuthController` - публичные `POST /api/auth/register` и
  `POST /api/auth/login`. Делегирует всё в `AuthService`.
- `OtpController` - `/api/otp/*` под `@RequiresAuth(roles = USER)`.
  Использует `OtpService` и `CurrentUserProvider`, чтобы получить
  идентификатор текущего пользователя.
- `AdminController` - `/api/admin/*` под `@RequiresAuth(roles = ADMIN)`.
  Завязан на `OtpConfigService` (`get` / `update`) и
  `UserAdminService` (список не-администраторов, удаление пользователя).
- `GlobalExceptionHandler` - `@RestControllerAdvice`, который
  превращает `NotFoundException`, `ConflictException`,
  `ForbiddenException`, `UnauthorizedException`, ошибки валидации
  Spring MVC и непредвиденные исключения в `ApiError` с нужным
  HTTP-статусом.

Бизнес-логики в контроллерах нет: они только разбирают HTTP-запрос,
валидируют DTO и вызывают сервис.

### Пакет `dto`

Тела запросов и ответов API. Все DTO - record-классы. От JPA-сущностей
DTO отделены, чтобы внутренняя схема БД и публичный контракт API
менялись независимо.

- `dto` (корень): `ApiError` - тело ошибки
  (`status`, `error`, `message`, опциональный `details`).
- `dto.auth`: `LoginRequest`, `RegisterRequest`, `RegisterResponse`,
  `TokenResponse`.
- `dto.otp`: `GenerateOtpRequest`, `GenerateOtpResponse`,
  `ValidateOtpRequest`, `ValidateOtpResponse`.
- `dto.admin`: `OtpConfigRequest`, `OtpConfigResponse`, `UserResponse`.

### Пакет `domain`

JPA-сущности `User`, `OtpCode`, `OtpConfig` (поля и индексы перечислены
выше в разделе "Таблицы БД") и перечисления:

- `OtpStatus` - `ACTIVE`, `EXPIRED`, `USED`.
- `DeliveryChannel` - `SMS`, `EMAIL`, `TELEGRAM`, `FILE`.
- `Role` - `ADMIN`, `USER`.

### Пакет `exception`

Доменные исключения, на которые опирается `GlobalExceptionHandler`:

- `NotFoundException` - 404 (нет пользователя, OTP-кода и т. п.).
- `ConflictException` - 409 (например, повторная регистрация
  администратора).
- `UnauthorizedException` - 401 (нет или невалидный JWT).
- `ForbiddenException` - 403 (роль не подходит, попытка удалить
  администратора и т. д.).

Бросаются из сервисов, JWT-фильтра и интерсептора авторизации.

### Пакет `repository`

DAO-слой поверх `JpaRepository`:

- `UserRepository` - поиск по `login`, проверка наличия администратора,
  выборка списка не-администраторов.
- `OtpCodeRepository` - поиск активного кода пользователя по значению,
  поиск кода по паре `(id, userId)`, удаление всех кодов пользователя.
- `OtpConfigRepository` - стандартный `JpaRepository<OtpConfig, Long>`.

Кроме определения запросов, ничего, кроме работы с сущностями,
репозитории не делают.

### Пакет `security` и подпакет `security.jwt`

Здесь собрана аутентификация (JWT) и проверка ролей.

В пакете `security`:

- `WebSecurityConfig` - регистрирует `JwtAuthenticationFilter`
  (`FilterRegistrationBean`) и подключает `AuthorizationInterceptor`
  на путях `/api/**`.
- `JwtAuthenticationFilter` - читает заголовок
  `Authorization: Bearer ...`, отдаёт его в `JwtParser` и при успехе
  кладёт `CurrentUser` в атрибуты запроса.
- `PasswordEncoderConfig` - bean `BCryptPasswordEncoder` для
  хеширования паролей.
- `CurrentUser` - record с `id`, `login`, `role`.
- `CurrentUserProvider` и реализация `RequestScopedCurrentUserProvider`
  - достают текущего пользователя из атрибутов запроса.
- `RequiresAuth` - аннотация над контроллером/методом со списком
  допустимых ролей.
- `AuthAttributes` - константы ключей в атрибутах запроса.
- `AuthorizationInterceptor` - `HandlerInterceptor`. Если у обработчика
  стоит `RequiresAuth`, проверяет роль текущего пользователя и при
  необходимости бросает `UnauthorizedException` или
  `ForbiddenException`.

В подпакете `security.jwt`:

- `JwtProperties` - record с `secret`, `ttl`, `issuer`.
- `JwtKeyConfig` - формирует `SecretKey` (HMAC-SHA256) из значения
  секрета.
- `JwtIssuer` и `JjwtIssuer` - выдача JWT по `userId`, `login`, `role`.
  Используется в `AuthServiceImpl`.
- `JwtParser` и `JjwtParser` - проверка подписи, издателя и срока
  действия токена. Используется в `JwtAuthenticationFilter`.
- `JwtClaims` и `IssuedToken` - record-результаты разбора и выдачи
  токена.

### Пакет `service` (интерфейсы)

Контракт бизнес-логики, на который опираются контроллеры:

- `AuthService` - `register`, `login`.
- `OtpService` - `generate`, `validate`, `deleteOwnedCode`.
- `OtpConfigService` - `get`, `update`.
- `UserAdminService` - `listNonAdminUsers`, `deleteUser`.

### Пакет `service.impl`

Реализации сервисов. Все методы, изменяющие БД, помечены
`@Transactional`.

- `AuthServiceImpl` - регистрация (с проверкой уникальности `login` и
  запретом второй регистрации администратора) и аутентификация
  (BCrypt + выпуск JWT через `JwtIssuer`).
- `OtpServiceImpl` - основной сервис. В конструктор приходит список
  всех бинов `OtpNotificationSender`, они сохраняются в
  `EnumMap<DeliveryChannel, OtpNotificationSender>` для быстрого
  доступа по каналу. `generate` создаёт код заданной длины и TTL
  (берёт значения из `OtpConfigService`), сохраняет его со статусом
  `ACTIVE` и вызывает нужный sender. `validate` ищет активный код
  пользователя по значению; если срок истёк - переводит в `EXPIRED`,
  при успехе - в `USED`. `deleteOwnedCode` удаляет код, проверив,
  что он принадлежит текущему пользователю.
- `OtpConfigServiceImpl` - работа с таблицей `otp_config`. При
  отсутствии записи создаёт её на основе `OtpProperties`. `update`
  позволяет администратору менять длину кода и TTL без перезапуска
  приложения.
- `UserAdminServiceImpl` - `listNonAdminUsers` возвращает всех
  пользователей с ролью `USER`; `deleteUser` сначала удаляет коды
  пользователя через `OtpCodeRepository.deleteByUserId`, затем самого
  пользователя. Удалить администратора нельзя - бросается
  `ForbiddenException`.

### Пакет `service.notification`

Реализация четырёх каналов доставки. Каналы подключаются по
интерфейсу `OtpNotificationSender`, поэтому новый канал добавляется
без изменений в `OtpServiceImpl`.

- `OtpNotificationSender` - интерфейс с `channel(): DeliveryChannel`
  и `send(NotificationPayload)`.
- `NotificationPayload` - record с данными для отправки:
  `userId`, `login`, `operationId`, `code`, `destination`,
  `expiresAt`.
- `FileOtpNotificationSender` (`FILE`) - дописывает строку с кодом в
  файл `operation-<operationId>-otp.txt` в каталоге
  `app.otp.file-output-dir`.
- `EmailOtpNotificationSender` (`EMAIL`) - SMTP через Jakarta Mail
  (Angus Mail), параметры из `app.email.*`.
- `SmsOtpNotificationSender` (`SMS`) - SMPP через jsmpp, параметры из
  `app.sms.smpp.*`. Проверялся на эмуляторе SMPPsim.
- `TelegramOtpNotificationSender` (`TELEGRAM`) - отправка через
  `telegrambots-client`. Параллельно крутится long-polling
  обработчик, отвечающий пользователю его `chat id`, который дальше
  передаётся в поле `destination`.

### Пакет `service.scheduler`

- `OtpExpirationScheduler` - `@Scheduled`-задача. С интервалом
  `app.otp.expiration-scan-interval` вызывает
  `OtpCodeRepository.markExpired(ACTIVE, EXPIRED, now)` и логирует
  количество переведённых строк, чтобы активные коды с истёкшим
  `expiresAt` не оставались в БД до следующего вызова `validate`.

## Локальный запуск

### 1. PostgreSQL

В корне репозитория находится готовый `docker-compose.yml`. Запуск:

```bash
docker compose up -d postgres
```

По умолчанию: `localhost:5432`, БД `otp_app`, пользователь `otp_user`, пароль
`otp_password` (эти же значения прописаны в `application.properties`).

### 2. Сборка и запуск

```bash
./gradlew bootRun
```

Сервер запускается на `http://localhost:8080`.

### 3. Конфигурация каналов доставки

По умолчанию все "внешние" каналы (Email / Telegram / SMS) отключены:
отправители стартуют в режиме `disabled` и при попытке отправки возвращают
`500` с сообщением о причине. Так приложение запускается без секретов, и
сразу можно проверить канал `FILE`.

Все настройки читаются стандартным механизмом Spring (`@Value`), поэтому
переопределить любое значение можно одним из штатных способов: через
переменные окружения, system properties (`-D...`) или правкой
`application.properties`.

#### Email (Jakarta Mail / Angus Mail)

```bash
EMAIL_USERNAME=otp@example.com \
EMAIL_PASSWORD=app-password \
EMAIL_FROM=otp@example.com \
MAIL_SMTP_HOST=smtp.gmail.com \
MAIL_SMTP_PORT=587 \
./gradlew bootRun
```

#### Telegram (`telegrambots-longpolling` + `telegrambots-client`)

1. У `@BotFather` получаем токен бота.
2. Запускаем приложение:
   ```bash
   TELEGRAM_BOT_TOKEN=123456:ABC-DEF... ./gradlew bootRun
   ```
3. Открываем диалог с ботом и отправляем любое сообщение (например `/start`).
   Обработчик long-polling отвечает значением **chat id**.
4. Этот chat id передаётся в поле `destination` при вызове
   `POST /api/otp/generate` с `channel=TELEGRAM`.

Логика эхо бота с отправкой `chat_id` нужна потому, что Telegram-бот не может
написать пользователю первым: пользователь сам должен инициировать диалог.

#### SMS (jsmpp + SMPP-эмулятор)

1. Скачиваем SMPPsim, запускаем
   `startsmppsim.sh` (или `.bat`).
2. В `config/smppsim.props` смотрим значения `SYSTEM_ID` и `PASSWORD`
   (по умолчанию `smppclient1` и `password`).
3. Запускаем приложение:
   ```bash
   SMPP_SYSTEM_ID=smppclient1 \
   SMPP_PASSWORD=password \
   ./gradlew bootRun
   ```
4. Веб-интерфейс SMPPsim (по умолчанию `http://localhost:88`) показывает
   принятые сообщения.

#### Прочая конфигурация

| Ключ | Назначение | Значение по умолчанию |
| --- | --- | --- |
| `app.security.jwt.secret` | секрет для подписи JWT | `some-secret` |
| `app.security.jwt.ttl` | TTL токена | `PT1H` |
| `app.otp.default-code-length` | длина кода при первом старте (4..10) | `6` |
| `app.otp.default-ttl-seconds` | время жизни кода в секундах (>= 10) | `300` |
| `app.otp.file-output-dir` | директория для файлов канала `FILE` | `.` |
| `app.otp.expiration-scan-interval` | период запуска шедулера, помечающего просроченные коды (ISO-8601 `Duration`) | `PT1M` |
| `app.otp.expiration-scan-initial-delay` | задержка перед первым запуском шедулера после старта приложения | `PT10S` |

## API

Все эндпоинты возвращают JSON. Ошибки приходят в общем формате `ApiError`
(`status`, `error`, `message`, опционально `details`).

| Метод | Путь | Доступ | Назначение |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | публичный | регистрация |
| `POST` | `/api/auth/login` | публичный | аутентификация, выдача JWT |
| `POST` | `/api/otp/generate` | `USER` | генерация и отправка OTP |
| `POST` | `/api/otp/validate` | `USER` | валидация OTP |
| `DELETE` | `/api/otp/codes/{id}` | `USER` | удаление собственного OTP-кода |
| `GET` | `/api/admin/otp-config` | `ADMIN` | текущая конфигурация OTP |
| `PUT` | `/api/admin/otp-config` | `ADMIN` | изменение `codeLength` / `ttlSeconds` |
| `GET` | `/api/admin/users` | `ADMIN` | список не-администраторов |
| `DELETE` | `/api/admin/users/{id}` | `ADMIN` | удаление пользователя и его кодов |

JWT передаётся в заголовке `Authorization: Bearer <token>`.

### Примеры запросов

#### Регистрация

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"login":"vlad","password":"123456"}'
```

Ответ `201 Created`:
```json
{ "id": 1, "login": "vlad", "role": "USER" }
```

> Если поле `role` отсутствует, назначается `USER`. Зарегистрировать второго
> администратора нельзя, в этом случае возвращается `409 Conflict`.

#### Аутентификация

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"login":"vlad","password":"123456"}'
```

Ответ:
```json
{
  "token":     "eyJhbGciOiJIUzI1...",
  "tokenType": "Bearer",
  "expiresAt": "2026-04-28T20:35:00Z"
}
```

#### Генерация OTP

```bash
curl -X POST http://localhost:8080/api/otp/generate \
  -H 'Authorization: Bearer eyJhbGciOiJIUzI1...' \
  -H 'Content-Type: application/json' \
  -d '{
        "operationId": "transfer-1234",
        "channel":     "FILE",
        "destination": null
      }'
```

Поле `destination` для канала `FILE` можно не передавать. Для остальных
каналов оно обязательно: email-адрес для `EMAIL`, числовой chat id для
`TELEGRAM`, номер абонента для `SMS`.

Ответ `201 Created`:
```json
{
  "otpId":       42,
  "operationId": "transfer-1234",
  "channel":     "FILE",
  "expiresAt":   "2026-04-28T20:30:00Z"
}
```

При `channel=FILE` рядом с проектом создаётся файл
`operation-transfer-1234-otp.txt`, содержащий токен

#### Валидация OTP

Для валидации достаточно самого кода: сервис ищет активный код текущего
пользователя с указанным значением.

```bash
curl -X POST http://localhost:8080/api/otp/validate \
  -H 'Authorization: Bearer eyJhbGciOiJIUzI1...' \
  -H 'Content-Type: application/json' \
  -d '{"code":"039184"}'
```

Возможные ответы (всегда `200 OK`):

```json
{ "valid": true,  "message": "Code accepted" }
{ "valid": false, "message": "Code expired" }
{ "valid": false, "message": "Invalid code" }
```

`Invalid code` приходит, если у пользователя нет активного кода с таким
значением: код не выпускался, уже использован или был помечен как
просроченный. При успешной валидации статус кода в БД меняется на `USED`,
повторно тот же код принят не будет.

#### Изменение конфигурации (администратор)

```bash
curl -X PUT http://localhost:8080/api/admin/otp-config \
  -H 'Authorization: Bearer <ADMIN_TOKEN>' \
  -H 'Content-Type: application/json' \
  -d '{"codeLength":8,"ttlSeconds":600}'
```

#### Список пользователей и удаление

```bash
curl http://localhost:8080/api/admin/users \
  -H 'Authorization: Bearer <ADMIN_TOKEN>'

curl -X DELETE http://localhost:8080/api/admin/users/2 \
  -H 'Authorization: Bearer <ADMIN_TOKEN>'
```

При удалении пользователя сразу удаляются все его OTP-коды. Удалить
администратора через этот эндпоинт нельзя, возвращается `403 Forbidden`.

#### Удаление собственного OTP-кода

```bash
curl -X DELETE http://localhost:8080/api/otp/codes/42 \
  -H 'Authorization: Bearer <USER_TOKEN>'
```

Удаляет код с указанным `id`, если он принадлежит текущему пользователю.
Если код не найден или принадлежит другому пользователю, возвращается
`404 Not Found`.

## Сценарии использования

### Основной сценарий: пользователь подтверждает операцию

1. `POST /api/auth/register` - пользователь регистрирует учётную запись.
2. `POST /api/auth/login` - пользователь получает JWT-токен.
3. `POST /api/otp/generate` с `operationId=transfer-1234`, `channel=EMAIL`,
   `destination=vlad@example.com`. Сервер сохраняет код со статусом
   `ACTIVE` и отправляет письмо.
4. Пользователь получает код в письме, делает `POST /api/otp/validate` с
   полученным кодом. Сервер отвечает `{"valid": true}`, код переходит в
   статус `USED`.

### Сценарий администратора

1. Администратор регистрируется (только один раз в системе).
2. `GET /api/admin/otp-config` - просмотр текущих настроек.
3. `PUT /api/admin/otp-config` - увеличение длины кода до 8 символов и TTL
   до 600 секунд.
4. `GET /api/admin/users` - просмотр списка пользователей.
5. `DELETE /api/admin/users/{id}` - удаление пользователя со всеми его
   кодами.

## Как протестировать код

Для базовой проверки работоспособности приложения дополнительные утилиты не
требуются: достаточно стандартного `curl` и набора запросов из раздела
[Примеры запросов](#примеры-запросов). Запросы можно выполнять как через `curl`, так и
через любой удобный HTTP-клиент (Postman, Bruno) -
тело запросов и заголовки совпадают с приведёнными в документации.

### Проверка каналов доставки

- **FILE** - наиболее простой случай: после `POST /api/otp/generate`
  появляется файл `operation-<id>-otp.txt`. Дополнительная настройка не
  требуется.
- **EMAIL** - для проверки можно настроить свой личный почтовый ящик, например, Яндекс-Почты
  для принятия сообщений по протоколу IMAP, подробнее в [документации от Яндекса](https://yandex.ru/support/yandex-360/customers/mail/ru/mail-clients/others).
- **TELEGRAM** - требуется реальный токен от `@BotFather` и Telegram-клиент,
  чтобы получить chat id.
- **SMS** - запускается SMPPsim, принятые сообщения отображаются в его
  веб-интерфейсе.
