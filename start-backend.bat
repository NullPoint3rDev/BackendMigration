@echo off
echo Starting Spring Boot Backend...
echo Backend will be available at: http://YOUR_IP:8084/api
echo.

cd WT2
call mvnw.cmd spring-boot:run
pause 