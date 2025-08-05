@echo off
echo ========================================
echo WeldTelecom Manual Start Script
echo ========================================
echo.

echo Checking current directory...
echo Current: %CD%
echo.

echo Checking Maven...
if exist mvnw.cmd (
    echo ✅ Maven wrapper found
    set MAVEN_CMD=mvnw.cmd
) else (
    echo ⚠️  Maven wrapper not found, checking mvn...
    mvn --version >nul 2>&1
    if %errorlevel% equ 0 (
        echo ✅ Maven found
        set MAVEN_CMD=mvn
    ) else (
        echo ❌ Maven not found! Please install Maven.
        pause
        exit /b 1
    )
)

echo.
echo Checking Frontend directory...
if exist "..\..\WebstormProjects\BetaFrontWT\package.json" (
    echo ✅ Frontend found at ..\..\WebstormProjects\BetaFrontWT\
    set FRONTEND_PATH=..\..\WebstormProjects\BetaFrontWT
) else if exist "..\WebstormProjects\BetaFrontWT\package.json" (
    echo ✅ Frontend found at ..\WebstormProjects\BetaFrontWT\
    set FRONTEND_PATH=..\WebstormProjects\BetaFrontWT
) else (
    echo ❌ Frontend not found! Please check the path.
    echo Looking for package.json in parent directories...
    dir /s /b package.json 2>nul
    pause
    exit /b 1
)

echo.
echo Starting Backend...
start "Backend" cmd /k "%MAVEN_CMD% spring-boot:run"

echo Waiting 10 seconds for backend to start...
timeout /t 10 /nobreak > nul

echo.
echo Starting Frontend...
cd %FRONTEND_PATH%
set REACT_APP_API_URL=http://95.172.58.219:8084/api
set HOST=0.0.0.0
set PORT=3001
start "Frontend" cmd /k "npm run start"

echo.
echo ========================================
echo Application started!
echo Backend and Frontend are running in separate windows.
echo Close those windows to stop the application.
echo ========================================
pause 