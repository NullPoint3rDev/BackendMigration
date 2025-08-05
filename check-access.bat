@echo off
setlocal enabledelayedexpansion

set EXTERNAL_IP=95.172.58.219
set LOCAL_IP=192.168.0.100

echo 🔍 Проверка доступности WeldTelecom приложения
echo ================================================
echo Внешний IP: %EXTERNAL_IP%
echo Локальный IP: %LOCAL_IP%
echo.

echo 📱 Проверка локального доступа:
echo ---------------------------------

echo -n Backend (локально): 
powershell -Command "try { Invoke-WebRequest -Uri 'http://%LOCAL_IP%:8084/api' -TimeoutSec 5 | Out-Null; Write-Host '✅ Доступен' } catch { Write-Host '❌ Недоступен' }"

echo -n Frontend (локально): 
powershell -Command "try { Invoke-WebRequest -Uri 'http://%LOCAL_IP%:3001' -TimeoutSec 5 | Out-Null; Write-Host '✅ Доступен' } catch { Write-Host '❌ Недоступен' }"

echo.

echo 🌐 Проверка внешнего доступа:
echo -----------------------------

echo -n Backend (внешне): 
powershell -Command "try { Invoke-WebRequest -Uri 'http://%EXTERNAL_IP%:8085/api' -TimeoutSec 5 | Out-Null; Write-Host '✅ Доступен' } catch { Write-Host '❌ Недоступен (возможно, не настроен проброс портов)' }"

echo -n Frontend (внешне): 
powershell -Command "try { Invoke-WebRequest -Uri 'http://%EXTERNAL_IP%:3001' -TimeoutSec 5 | Out-Null; Write-Host '✅ Доступен' } catch { Write-Host '❌ Недоступен (возможно, не настроен проброс портов)' }"

echo.

echo 🌍 Текущий внешний IP:
echo ----------------------
for /f "tokens=*" %%i in ('powershell -Command "Invoke-WebRequest -Uri 'https://ifconfig.me' | Select-Object -ExpandProperty Content"') do set CURRENT_IP=%%i
echo Текущий: %CURRENT_IP%
echo Ожидаемый: %EXTERNAL_IP%

if "%CURRENT_IP%"=="%EXTERNAL_IP%" (
    echo ✅ IP совпадает
) else (
    echo ⚠️  IP изменился! Обновите настройки.
)

echo.

echo 📋 Следующие шаги:
echo ==================
echo 1. Если локальный доступ ❌ - запустите приложение:
echo    start-all.bat
echo.
echo 2. Если внешний доступ ❌ - настройте проброс портов:
echo    - Откройте настройки роутера (192.168.0.1)
echo    - Найдите 'Port Forwarding'
echo    - Добавьте правила для портов 8085 и 3001
echo    - Подробности в файле PORT_FORWARDING_GUIDE.md
echo.
echo 3. После настройки проверьте доступность:
echo    Frontend: http://%EXTERNAL_IP%:3001
echo    Backend:  http://%EXTERNAL_IP%:8085/api

pause 