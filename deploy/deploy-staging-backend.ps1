# Ручной деплой backend на Staging (то же, что job deploy в .github/workflows/staging.yml)
# Запуск: PowerShell от администратора не обязателен; Docker Desktop должен быть запущен.

$ErrorActionPreference = 'Stop'

Set-Location 'C:\WTStaging\BackendStaging'
git fetch origin main
git checkout main
git pull origin main

Set-Location 'C:\WTStaging\BackendStaging\deploy'
docker compose up -d --build backend
docker compose up -d prometheus grafana loki promtail postgres-exporter

Write-Host "Done. Check: docker compose ps"
