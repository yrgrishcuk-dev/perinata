# Антенатальный помощник

Пилотный MVP персонального помощника для беременных: бот и мини-приложение в Telegram и MAX, веб-кабинет для врача. Одна организация, данные в PostgreSQL, развёртывание в российском облаке.

Реализовано по плану `1793021522670-antenatal-assistant-telegram-mvp.md`.

## Что умеет

Пациентка:
- привязывается к карточке по одноразовому QR-коду врача;
- видит ИМТ и коридор прибавки, которые рассчитаны по росту и весу, внесённым врачом;
- видит рацион с КБЖУ для своей категории ИМТ;
- заполняет ежедневный дневник: АД, пульс, вес, глюкоза с контекстом;
- проходит анкету из 9 тревожных симптомов;
- ведёт счётчик схваток и получает подсказку, когда ехать в роддом;
- читает развитие плода и питание по неделям;
- видит записи на приём, подтверждает участие, сообщает «не смогу прийти», получает напоминание за 24 часа и скачивает `.ics`;
- переписывается с врачом внутри приложения.

Врач:
- создаёт карточки пациенток вручную (ФИО, возраст, ПДР, рост, вес, комментарий);
- генерирует QR для привязки и отзывает привязку;
- видит график прибавки веса, дневник, анкеты, схватки;
- получает нейтральные уведомления в Telegram или MAX и разбирает события в кабинете;
- настраивает пороги АД и глюкозы для конкретной пациентки;
- ведёт календарь и личные события, в том числе с привязкой к пациентке;
- записывает пациентку на приём, видит подтверждение или отказ, отменяет и переносит запись;
- видит календарь с окном ПДР ±1 день, цветами по сроку, риском и переходом в карточку;
- работает с телефона: нижнее меню, компактный календарь и карточки вместо таблиц;
- проходит встроенное обучение и чек-лист первых шагов;
- ведёт переписку с пациенткой.

Администратор:
- создаёт и отключает врачей, сбрасывает пароли;
- смотрит аудит действий.

## Стек

- Backend: Java 21, Spring Boot 3.3, Spring Security, Spring Data JPA, Flyway, PostgreSQL 16.
- Frontend: React 18, TypeScript, Vite, React Router.
- Telegram: Bot API, WebApp, deep links.
- MAX: Bot API `platform-api2.max.ru`, MAX Bridge, deep links, webhook.
- Деплой: Docker Compose, Caddy (TLS), ежедневный `pg_dump`.

## Структура

```
backend/   Spring Boot API, Telegram, MAX, планировщик
web/       React SPA: мини-приложение и кабинет
deploy/    Caddyfile, backup.sh
docs/      клинические пороги и чек-лист согласования
```

## Быстрый старт (Docker)

1. Создайте `.env` из примера и заполните значения:

```bash
cp .env.example .env
```

2. Для локальной проверки без Telegram установите `DEV_MODE=true` и `DOMAIN=localhost` (Caddy с localhost выпустит локальный сертификат; для простоты можно открыть приложение напрямую на `http://localhost:8080` после сборки).

3. Запустите:

```bash
docker compose up -d --build
```

Приложение: `http://localhost:8080` (или `https://<DOMAIN>`).
Кабинет: `/cabinet/login`.

## Окружение на этой машине

Инструменты установлены портативно в `C:\work\.tools`:

- Temurin JDK 21.0.12.1;
- Apache Maven 3.9.16;
- Node.js 20.18.0;
- PostgreSQL 16.4, данные в `C:\work\.data\pg` (порт 5432, БД `antenatal`, роль `antenatal`).

Готовые скрипты:

```powershell
# собрать фронтенд, прогнать тесты и пересобрать jar
.\scripts\build-dev.ps1

# запустить PostgreSQL (если нужно) и приложение на :8080
.\scripts\run-dev.ps1

# остановить PostgreSQL
.\scripts\stop-pg.ps1
```

Приложение: `http://localhost:8080/app`.
Кабинет: `http://localhost:8080/cabinet/login`.
Демо-врач: `doctor@example.com` / `doctor123`.
Демо-админ: `admin@example.com` / `admin123456`.

Скрипты рассчитаны на текущую раскладку `C:\work\.tools` и `C:\work\.data`. На другой машине установите инструменты отдельно или поправьте пути в `scripts\dev-env.ps1`.

## Локальная разработка

Нужны Java 21, Maven 3.9+, Node 20+, PostgreSQL 16.

```bash
# база
createdb antenatal

# backend
cd backend
DATABASE_URL=jdbc:postgresql://localhost:5432/antenatal \
DATABASE_USER=postgres \
DATABASE_PASSWORD=postgres \
DEV_MODE=true \
PUBLIC_BASE_URL=http://localhost:5173 \
mvn spring-boot:run

# frontend (в другом окне)
cd web
npm install
npm run dev
```

Vite проксирует `/api` и `/l` на `http://localhost:8080`. Откройте `http://localhost:5173/app`.

## Демо-режим

При `DEV_MODE=true` при старте создаются:

- врач: `doctor@example.com` / `doctor123` (роль `DOCTOR`);
- пациентка: `Иванова Анна Сергеевна`, код карточки `DEMO1234`, привязанный демо-аккаунт;
- администратор — только если заданы `ADMIN_EMAIL` и `ADMIN_PASSWORD`.

Вход в кабинет: `/cabinet/login`. Мини-приложение в dev-режиме само получает демо-сессию пациентки.

## Настройка Telegram

1. Создайте бота у `@BotFather`, получите токен и username (без `@`).
2. Заполните в `.env`:
   - `TELEGRAM_BOT_TOKEN`;
   - `TELEGRAM_BOT_USERNAME`;
   - `TELEGRAM_WEBHOOK_SECRET` (случайная строка);
   - `PUBLIC_BASE_URL=https://<домен>`.
3. Домен должен быть доступен по HTTPS. В `@BotFather` выполните `/setdomain` и укажите домен — это нужно для WebApp-кнопок.
4. Установите webhook:

```bash
curl "https://api.telegram.org/bot<TOKEN>/setWebhook?url=https://<DOMAIN>/api/telegram/webhook/<SECRET>&secret_token=<SECRET>"
```

5. Кнопка меню:

```bash
curl -X POST "https://api.telegram.org/bot<TOKEN>/setChatMenuButton" \
  -H "Content-Type: application/json" \
  -d '{"menu_button":{"type":"web_app","text":"Открыть","web_app":{"url":"https://<DOMAIN>/app"}}}'
```

Проверка webhook:

```bash
curl "https://api.telegram.org/bot<TOKEN>/getWebhookInfo"
```

## Настройка MAX

1. Зарегистрируйте профиль организации, ИП или самозанятого в [MAX для партнёров](https://business.max.ru/self) — нужен резидент РФ.
2. Создайте бота и получите токен и username (без `@`).
3. В настройках бота укажите URL мини-приложения:
   ```
   https://<ДОМЕН>/app
   ```
   Выберите кнопку открытия «Открыть» и сохраните.
4. Заполните в `.env`:
   ```dotenv
   MAX_BOT_TOKEN=<токен>
   MAX_BOT_USERNAME=<username без @>
   MAX_WEBHOOK_SECRET=<случайная hex-строка>
   MAX_API_BASE_URL=https://platform-api2.max.ru
   ```
5. Перезапустите приложение:
   ```bash
   docker compose up -d
   ```
6. Подпишитесь на обновления. На сервере уже установлены корневой и промежуточный сертификаты Минцифры, поэтому используйте серверный скрипт:
   ```bash
   ssh root@<СЕРВЕР> "bash /opt/antenatal/deploy/set-max-webhook.sh"
   ```
   Скрипт берёт `MAX_*` из `/opt/antenatal/.env`, ставит webhook и выводит текущие подписки.
   
   Альтернатива вручную:
   ```bash
   curl -X POST "https://platform-api2.max.ru/subscriptions" \
     -H "Authorization: <MAX_BOT_TOKEN>" \
     -H "Content-Type: application/json" \
     -d '{"url":"https://<ДОМЕН>/api/max/webhook/<MAX_WEBHOOK_SECRET>","update_types":["message_created","bot_started"],"secret":"<MAX_WEBHOOK_SECRET>"}'
   ```
7. Проверьте подписки:
   ```bash
   curl "https://platform-api2.max.ru/subscriptions" -H "Authorization: <MAX_BOT_TOKEN>"
   ```
8. Откройте бота в MAX, нажмите Start или отправьте `/start`. Для привязки пациентки можно использовать тот же QR: лендинг предложит Telegram и MAX.

> MAX API использует сертификаты Минцифры. Они добавлены в `deploy/certs` и в Java truststore Docker-образа; на хосте — в системный truststore. Без этого TLS-соединение к `platform-api2.max.ru` завершается ошибкой проверки сертификата.

Мини-приложение MAX должно быть доступно по HTTPS. MAX Bridge подключается в `web/index.html`; данные `initData` проверяются на бэкенде по HMAC-SHA256 с токеном MAX-бота. Бот проходит модерацию MAX; до этого он может быть недоступен другим пользователям.

## Как работает привязка

1. Врач создаёт карточку и нажимает «QR для привязки».
2. Бэкенд выпускает одноразовый токен (TTL 15 минут, хранится SHA-256).
3. QR ведёт на `https://<домен>/l/<token>`; страница не содержит ПДн и предлагает открыть Telegram или MAX.
4. Бот получает `/start <token>` и открывает мини-приложение с приглашением.
5. Пациентка видит ФИО, возраст и ПДР, подтверждает согласие.
6. Создаётся привязка; токен гасится.

Медданные не отправляются в чат. Уведомления нейтральные: «Событие по карточке A7K2, откройте кабинет».

## API (основное)

Публичное:
- `GET /l/{token}` — landing для QR;
- `POST /api/telegram/webhook/{secret}` — webhook Telegram;
- `POST /api/max/webhook/{secret}` — webhook MAX;
- `POST /api/patient/session` — обмен Telegram `initData` на токен;
- `POST /api/patient/session/max` — обмен MAX `initData` на токен.

Пациентка (Bearer):
- `GET /api/patient/me`, `GET /api/patient/profile`;
- `GET /api/patient/invite/{token}`, `POST /api/patient/invite/{token}/confirm`;
- `GET /api/patient/diary`, `POST /api/patient/diary`, `GET /api/patient/diary/today`;
- `GET /api/patient/appointments`, `POST /api/patient/appointments/{id}/confirm|decline`;
- `GET /api/patient/gain`;
- `GET /api/patient/content/current`, `GET /api/patient/content/{week}`;
- `POST /api/patient/symptoms`, `GET /api/patient/symptoms/history`;
- `GET/POST /api/patient/contractions`, `POST /api/patient/contractions/close`, `POST /api/patient/contractions/waters`;
- `GET/POST /api/patient/messages`, `POST /api/patient/messages/read`;
- `GET /api/patient/alerts`;
- `POST /api/patient/consent/revoke` — отзыв согласия и закрытие доступа.

Врач (Bearer):
- `GET /api/doctor/dashboard`;
- `GET/POST /api/doctor/patients`, `GET/PATCH /api/doctor/patients/{id}`;
- `POST /api/doctor/patients/{id}/link`, `POST /api/doctor/patients/{id}/revoke-binding`, `POST /api/doctor/patients/{id}/close`;
- `GET /api/doctor/patients/{id}/diary|alerts|symptoms|contractions|messages`;
- `POST /api/doctor/patients/{id}/messages`, `POST .../messages/read`;
- `PUT /api/doctor/patients/{id}/thresholds`;
- `GET /api/doctor/alerts`, `POST /api/doctor/alerts/{id}/ack|close`;
- `GET/POST /api/doctor/calendar/events`, `PUT/DELETE /api/doctor/calendar/events/{id}`;
- `GET/POST /api/doctor/appointments`, `PUT /api/doctor/appointments/{id}`, `POST /api/doctor/appointments/{id}/cancel`;
- `POST /api/doctor/messenger-link`.

Админ (Bearer):
- `GET/POST /api/admin/doctors`, `PATCH /api/admin/doctors/{id}`, `POST /api/admin/doctors/{id}/reset-password`;
- `GET /api/admin/audit`, `GET /api/admin/patients`.

## Клинические правила

Значения по умолчанию лежат в `backend/src/main/resources/db/migration/V2__seed.sql`:

- АД: 140/90 — внимание, 160/110 — критично;
- глюкоза натощак: 5,1 / 7,0 ммоль/л;
- глюкоза через 2 часа: 6,7 / 11,1 ммоль/л;
- прибавка веса: IOM 2009, 4 категории ИМТ;
- схватки: до 37 недель ≥ 4/час — критично; 37+ недель ≥ 4/час — внимание; каждые 5 минут по 60 секунд в течение часа — «пора в роддом»;
- отошли воды — критично.

Перед пилотом клиника должна утвердить пороги, таблицу прибавки, правила схваток и тексты. Чек-лист: `docs/clinical-signoff.md`.

## Безопасность и 152-ФЗ

- Пароли — BCrypt; сессии — непрозрачные токены, SHA-256 в БД.
- Проверка Telegram `initData` по HMAC-SHA256, срок `auth_date`.
- RBAC: врач видит только своих пациенток, админ — всех в клинике.
- Аудит: вход, создание/изменение карточки, привязка, согласие, алерты, сообщения.
- Медданные — только в PostgreSQL; мессенджер используется как транспорт уведомлений.
- TLS терминируется Caddy; PostgreSQL не публикуется наружу.
- `npm audit --omit=dev` — 0 уязвимостей; предупреждения `npm audit` относятся к dev-серверу Vite/esbuild и не попадают в production-сборку.
- Бэкапы ежедневно, шифруются AES-256 (`BACKUP_PASSPHRASE`), хранение 30 дней. Восстановление:

```bash
openssl enc -d -aes-256-cbc -pbkdf2 -pass env:BACKUP_PASSPHRASE \
  -in backup/antenatal-YYYYMMDD-HHMM.sql.gz.enc | gunzip | psql "$DATABASE_URL"
```

Перед эксплуатацией с реальными пациентками:
- определить оператора ПДн и подать уведомление в Роскомнадзор;
- утвердить текст согласия и политику конфиденциальности;
- оценить необходимость регистрации ПО как медицинского изделия;
- провести проверку восстановления из бэкапа и аудит доступов.

## Тесты

```bash
cd backend
mvn test
```

Покрыты чистые расчёты: ИМТ, срок и коридор прибавки, пороги АД/глюкозы, правила схваток, токены.

## Ограничения MVP

- MAX реализован на уровне бота, мини-приложения, привязки и уведомлений. Для запуска нужен профиль организации/ИП/самозанятого в MAX, созданный бот, настроенный URL мини-приложения и webhook. Без этих данных MAX-функции выключены.
- Нет круглосуточной дежурной смены и SLA; пациентка всегда получает 103/112.
- Контент по неделям и рационы — демонстрационные, требуют замены на утверждённые клиникой.
- Нет интеграции с МИС/ЕГИСЗ.
- Синхронизация календаря с Google/Outlook — только через скачивание `.ics`, без OAuth и webcal.
- Нет внешнего QR пациентки для другого врача — следующая итерация.
- Уведомления отправляются только при запущенном планировщике; несколько инстансов не поддерживаются (single instance).
