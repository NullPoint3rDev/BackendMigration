@echo off
echo ========================================
echo WeldTelecom - Start with IntelliJ IDEA
echo ========================================
echo.

echo This script will help you start the application using IntelliJ IDEA
echo.

echo Step 1: Open IntelliJ IDEA
echo Step 2: Open the project: C:\Users\Stas\IdeaProjects\WT2
echo Step 3: Find and run: src\main\java\org\alloy\WeldingApplication.java
echo.

echo Starting Frontend in separate window...
cd ..\..\WebstormProjects\BetaFrontWT
set REACT_APP_API_URL=http://95.172.58.219:8084/api
set HOST=0.0.0.0
set PORT=3001
start "Frontend" cmd /k "npm run start"

echo.
echo ========================================
echo Instructions:
echo 1. Backend: Run WeldingApplication.java in IntelliJ
echo 2. Frontend: Running in separate window
echo 3. Access: http://95.172.58.219:3001
echo ========================================
pause 