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
- TCP 3050 — Grafana (мониторинг, в пределах локальной сети)

## Мониторинг (Prometheus + Grafana + Loki)

Стек поднимается тем же `docker compose up -d --build` и состоит из:

| Сервис | Назначение | Порт |
|--------|------------|------|
| Grafana | дашборды и логи | `GRAFANA_HTTP_PORT` (по умолчанию **3050**), доступна по LAN: `http://<ip-сервера>:3050` |
| Prometheus | метрики | только внутри docker-сети (наружу **не** публикуется) |
| Loki | хранилище логов | только внутри docker-сети |
| Promtail | сбор логов в Loki | только внутри docker-сети |
| postgres-exporter | метрики PostgreSQL | только внутри docker-сети |

Actuator backend вынесен на отдельный management-порт **9100**, который **не публикуется** наружу — его опрашивает только Prometheus внутри docker-сети (`/actuator/prometheus`).

Порядок старта управляется healthcheck'ами: `prometheus` и `frontend` ждут готовности `backend` (TCP-проверка порта 9100), `grafana` — готовности `prometheus` и `loki`, `promtail` — готовности `loki`. Backend на staging стартует не мгновенно, поэтому у его healthcheck задан `start_period: 120s`.

### Что отслеживается

- **Backend (Spring Boot Actuator):** JVM (heap, GC, потоки), HTTP (RPS, латентность p95, статусы), пул HikariCP.
- **Кастомные метрики WT2:**
  - `wt2_online_sessions` — пользователей онлайн (heartbeat за последние 11 минут);
  - `wt2_tcp_active_connections` — активные TCP-соединения сварочных аппаратов (archive listener);
  - `wt2_unknown_mac_total` — пакеты от неизвестных/неразрешённых MAC;
  - `wt2_report_generation_total{type,source,status}` + `wt2_report_generation_duration_seconds` — генерация отчётов (ручная и авто, по 4 типам, с длительностью и ошибками);
  - `wt2_backend_errors_total` — необработанные исключения (HTTP 5xx).
- **Активность по разделам (приближение):** метрика `http_server_requests_seconds_count` с разбивкой по `uri` (топ API-эндпоинтов).
- **PostgreSQL:** доступность, соединения, размеры (через postgres-exporter).
- **Windows-хост:** CPU, память, свободное место на дисках (через `windows_exporter`, см. ниже).
- **Логи в Loki:** stdout контейнеров `backend`/`frontend`, файлы `welding-devices.log` и `errors.log` из тома `backend-logs`.

### Первый вход в Grafana

1. Задайте в `.env`:
   ```env
   GRAFANA_HTTP_PORT=3050
   GRAFANA_ADMIN_USER=admin
   GRAFANA_ADMIN_PASSWORD=ваш-надёжный-пароль
   ```
2. Откройте `http://<ip-сервера>:3050` из локальной сети.
3. Войдите под `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD`.
4. Источники данных (Prometheus, Loki) и дашборды подключаются автоматически (provisioning). Папка: **WT2 Staging**. Готовые дашборды:
   - **WT2 Staging — Обзор** — сводный (здоровье, API, аппараты, отчёты, ошибки, хост);
   - **WT2 — JVM / Spring Boot** — память, GC, потоки, CPU, HikariCP;
   - **WT2 — PostgreSQL** — соединения, транзакции, строки, cache hit, deadlocks;
   - **WT2 — Windows-хост** — CPU, память, диски, сеть, служба PostgreSQL.

Дашборды JVM/PostgreSQL/Windows построены под метрики используемых экспортёров (аналоги community-дашбордов Micrometer/postgres_exporter/windows_exporter, но с корректными именами метрик и привязкой к provisioned-датасорсу).

### Установка windows_exporter (вручную, на хосте)

`windows_exporter` — это служба Windows, она **не** запускается в Docker.

1. Скачайте `.msi` со страницы релизов: <https://github.com/prometheus-community/windows_exporter/releases>
2. Установите (служба слушает порт **9182**), например с нужными коллекторами:
   ```powershell
   msiexec /i windows_exporter-<версия>-amd64.msi ENABLED_COLLECTORS="cpu,cs,logical_disk,memory,net,os,service"
   ```
3. Проверьте: `http://localhost:9182/metrics`
4. В firewall разрешите порт **9182** для адреса Docker (host-gateway). Prometheus обращается к нему как `host.docker.internal:9182` — target уже прописан в `monitoring/prometheus/prometheus.yml`.

### Хранение данных

- Prometheus: **30 дней** (`PROMETHEUS_RETENTION`).
- Loki: **14 дней** (в `monitoring/loki/loki-config.yml`).
- Данные в docker-томах `prometheus-data`, `loki-data`, `grafana-data`.

### Алерты (Grafana UI, без внешних уведомлений)

Правила Prometheus в `monitoring/prometheus/alert.rules.yml`:

- **BackendDown** — backend недоступен;
- **ApiHttp5xxSpike** — всплеск 5xx на API;
- **PostgresDown** — PostgreSQL недоступен;
- **WindowsDiskSpaceLow** — на диске Windows < 10% свободно;
- **ReportGenerationErrors** — ошибки генерации отчётов.

## Остановка

```powershell
docker compose down
```

Данные PostgreSQL остаются на хосте.

## CI/CD (автодеплой с GitHub)

При **push в ветку `main`**:

| Репозиторий | CI (облако GitHub) | CD (сервер `C:\WTStaging`) |
|-------------|-------------------|---------------------------|
| **BackendStaging** | `mvn test`, gitleaks, проверка `docker-compose` | `actions/checkout` на runner + `docker compose` (код из GitHub; `.env` только с `C:\WTStaging\...\deploy\.env`) |
| **FrontendStaging** | `npm ci` + `build --mode staging`, gitleaks | checkout frontend в `_work` + `docker compose` из `C:\WTStaging\...\deploy` (compose обновляет backend deploy) |

На **pull request** в `main` выполняется только CI (деплой не запускается).

Workflow-файлы:

- `BackendStaging/.github/workflows/staging.yml`
- `FrontendStaging/.github/workflows/staging.yml`

### Однократная настройка: GitHub Actions runner на Staging

1. На сервере откройте репозиторий **BackendStaging** на GitHub → **Settings → Actions → Runners → New self-hosted runner → Windows**.
2. Скачайте и установите runner по инструкции GitHub (папка, например `C:\actions-runner`).
3. При регистрации добавьте **дополнительную метку** (label): `wt2-staging`  
   Итоговые метки runner: `self-hosted`, `windows`, `wt2-staging` — они указаны в workflow.
4. Запустите runner как службу (команда из инструкции GitHub: `.\run.cmd` для теста или установка службы).  
   **CD не делает `git pull` в `C:\WTStaging`**. Код для сборки берётся через `actions/checkout` в каталог runner `_work`.

   **Docker (обязательно):** deploy вызывает `docker compose`. Ошибка  
   `permission denied ... npipe:////./pipe/docker_engine`  
   значит учётная запись **службы runner** не имеет доступа к Docker Desktop.

   - Запустите **Docker Desktop** на сервере (или Docker Engine).
   - `services.msc` → служба **GitHub Actions Runner** → **Вход в систему** → укажите **того же пользователя Windows**, под которым вы обычно работаете с Docker (не `NETWORK SERVICE` / не `Local System`).
   - **Управление компьютером** → **Группы** → **docker-users** → добавьте этого пользователя → перезагрузка или выход/вход.
   - Перезапустите службу runner. Для Docker Desktop часто нужна хотя бы одна интерактивная сессия этого пользователя (автовход или RDP).

   Проверка под этим пользователем (PowerShell):

   ```powershell
   docker version
   docker compose -f C:\WTStaging\BackendStaging\deploy\docker-compose.yml ps
   ```

5. Обязательно на сервере: `C:\WTStaging\BackendStaging\deploy\.env` (секреты staging, **не** в git).  
   Клоны в `C:\WTStaging\...` нужны для **ручного** деплоя (`deploy-staging-*.ps1`), не для GitHub Actions.
6. (Опционально) Клоны для ручной работы:
   - `C:\WTStaging\BackendStaging`
   - `C:\WTStaging\FrontendStaging`

Проверка: в GitHub → **Actions** после push в `main` должны быть зелёные jobs **test/build**, **secrets**, **deploy**.

**FrontendStaging** не клонирует `BackendStaging` (у `GITHUB_TOKEN` нет доступа к другому private-репо — в логе `Repository not found`). `docker-compose.yml` и monitoring лежат в `C:\WTStaging\BackendStaging\deploy` и **обновляются при каждом backend deploy**. Перед первым frontend deploy выполните хотя бы один успешный backend deploy (или скопируйте папку `deploy` вручную).

### Ручной деплой (без GitHub)

```powershell
# только backend
C:\WTStaging\BackendStaging\deploy\deploy-staging-backend.ps1

# только frontend
C:\WTStaging\BackendStaging\deploy\deploy-staging-frontend.ps1
```

### Обновление вручную (как раньше)

```powershell
cd C:\WTStaging\BackendStaging
git pull
cd ..\FrontendStaging
git pull
cd ..\BackendStaging\deploy
docker compose up -d --build
```
