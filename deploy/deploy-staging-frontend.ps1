# Ручной деплой только frontend (то же, что job deploy в FrontendStaging workflow)
# Запуск из любой папки; Docker Desktop должен быть запущен.

$ErrorActionPreference = 'Stop'

Set-Location 'C:\WTStaging\FrontendStaging'
git fetch origin main
git checkout main
git pull origin main

Set-Location 'C:\WTStaging\BackendStaging\deploy'
docker compose up -d --build frontend

Write-Host "Done. Check: docker compose ps"
