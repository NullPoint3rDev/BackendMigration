@echo off
echo Starting Spring Boot Backend...
echo Backend will be available at: http://YOUR_IP:8084/api
echo.

REM Backend находится в текущей папке
REM Проверяем наличие mvnw.cmd, если нет - используем mvn
if exist mvnw.cmd (
    call mvnw.cmd spring-boot:run
) else (
    echo Maven wrapper not found, using mvn command...
    mvn spring-boot:run
)
pause 