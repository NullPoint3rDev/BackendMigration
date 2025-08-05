@echo off
echo ========================================
echo Installing Maven for WeldTelecom
echo ========================================
echo.

echo Checking if Maven is already installed...
mvn --version >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Maven is already installed!
    mvn --version
    pause
    exit /b 0
)

echo Maven not found. Installing...

REM Create Maven directory
if not exist "%USERPROFILE%\maven" mkdir "%USERPROFILE%\maven"

REM Download Maven
echo Downloading Maven...
powershell -Command "Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/3.9.5/binaries/apache-maven-3.9.5-bin.zip' -OutFile '%USERPROFILE%\maven\maven.zip'"

REM Extract Maven
echo Extracting Maven...
powershell -Command "Expand-Archive -Path '%USERPROFILE%\maven\maven.zip' -DestinationPath '%USERPROFILE%\maven' -Force"

REM Add to PATH
echo Adding Maven to PATH...
setx PATH "%PATH%;%USERPROFILE%\maven\apache-maven-3.9.5\bin"

echo.
echo ========================================
echo Maven installation completed!
echo Please restart your command prompt.
echo ========================================
pause 