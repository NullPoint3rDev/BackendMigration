# WT2 Staging (Windows + PostgreSQL на хосте)

Оркестрация Docker для двух репозиториев:

- [BackendStaging](https://github.com/NullPoint3rDev/BackendStaging) — этот репозиторий
- [FrontendStaging](https://github.com/NullPoint3rDev/FrontendStaging) — UI

## Структура на сервере

Клонируйте оба репозитория **в одну родительскую папку**:

```
C:\WT2\
  BackendStaging\
    deploy\          ← docker compose запускается отсюда
    Dockerfile
    ...
  FrontendStaging\
    Dockerfile
    ...
```

```powershell
cd C:\WT2
git clone https://github.com/NullPoint3rDev/BackendStaging.git
git clone https://github.com/NullPoint3rDev/FrontendStaging.git
```

Если фронт лежит в другом месте, укажите путь в `.env`:

```env
FRONTEND_BUILD_CONTEXT=D:\repos\FrontendStaging
```

## Требования

- Windows с [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- PostgreSQL на Windows (служба запущена)
- База `weldtelecom`

## PostgreSQL на хосте

```sql
CREATE DATABASE weldtelecom ENCODING 'UTF8';
```

Backend в контейнере подключается через `host.docker.internal`.

## Запуск

```powershell
cd C:\WT2\BackendStaging\deploy
copy .env.example .env
# отредактируйте .env — пароль БД, APP_JWT_SECRET

docker compose up -d --build
```

- UI: http://localhost (порт `FRONTEND_HTTP_PORT`, по умолчанию 80)
- API: http://localhost:8085/api (если проброшен `BACKEND_HTTP_PORT`)
- TCP аппаратов: порт `WELDING_ARCHIVE_SERVER_PORT` (по умолчанию 3003) на IP сервера

## Первый вход

| Поле | Значение |
|------|----------|
| Логин | `Administrator` |
| Пароль | `Admin123!@#` |

При первом старте: только предприятие **Компания Alloy** (без подразделений) и учётная запись администратора.

## Логи

```powershell
docker compose exec backend ls -la /app/logs
docker compose exec backend tail -f /app/logs/unknown-mac.log
```

## Firewall Windows

- TCP 80 — веб
- TCP 8085 — API (опционально)
- TCP 3003 — сварочные аппараты

## Остановка

```powershell
docker compose down
```

Данные PostgreSQL остаются на хосте.

## Обновление

```powershell
cd C:\WT2\BackendStaging
git pull
cd ..\FrontendStaging
git pull
cd ..\BackendStaging\deploy
docker compose up -d --build
```
