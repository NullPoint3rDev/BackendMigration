@echo off
echo ========================================
echo Тестирование данных от платы
echo ========================================
echo.

echo Ожидаемый IP платы: 95.172.58.219
echo Ожидаемый MAC платы: 8CAAB579425A
echo.

echo Отправка тестовых данных на сервер...
echo.

REM Отправляем тестовые данные через curl
echo Тест 1: Простые данные
curl -X POST http://95.172.58.219:3000 -H "Content-Type: text/plain" -d "8CAAB579425A:TEMP=45;POWER=100;SPEED=50;STATUS=WORKING"

echo.
echo.

echo Тест 2: Данные с HTTP заголовками
curl -X POST http://95.172.58.219:3000 -H "Content-Type: text/plain" -H "User-Agent: WeldingDevice/1.0" -d "8CAAB579425A:TEMP=52;POWER=85;SPEED=30;STATUS=IDLE;MEMORY=75"

echo.
echo.

echo Тест 3: Данные в теле запроса
curl -X POST http://95.172.58.219:3000 -H "Content-Type: application/x-www-form-urlencoded" -d "data=8CAAB579425A:TEMP=38;POWER=120;SPEED=60;STATUS=ERROR;ERROR=OVERTEMP"

echo.
echo.

echo ========================================
echo Тестирование завершено!
echo Проверьте логи backend для подтверждения получения данных
echo Проверьте страницу мониторинга: http://95.172.58.219:3001/device-monitor
echo ========================================
pause 