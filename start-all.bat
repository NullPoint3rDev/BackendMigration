@echo off
echo Starting WeldTelecom Application...
echo Backend: http://YOUR_IP:8084/api
echo Frontend: http://YOUR_IP:3001
echo.

REM Start backend in background
echo Starting Backend...
REM Backend находится в текущей папке
start "Backend" cmd /k "call mvnw.cmd spring-boot:run"

REM Wait for backend to start
timeout /t 10 /nobreak > nul

REM Start frontend
echo Starting Frontend...
cd WebstormProjects\BetaFrontWT
set REACT_APP_API_URL=http://95.172.58.219:8084/api
set HOST=0.0.0.0
set PORT=3001
start "Frontend" cmd /k "call npm run start"

echo Application started!
echo Backend and Frontend are running in separate windows.
echo Close those windows to stop the application.
pause 