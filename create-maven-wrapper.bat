@echo off
echo ========================================
echo Creating Maven Wrapper for WeldTelecom
echo ========================================
echo.

echo Downloading Maven Wrapper files...

REM Download mvnw.cmd
powershell -Command "Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/takari/maven-wrapper/master/mvnw.cmd' -OutFile 'mvnw.cmd'"

REM Download mvnw
powershell -Command "Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/takari/maven-wrapper/master/mvnw' -OutFile 'mvnw'"

REM Download .mvn/wrapper/maven-wrapper.properties
if not exist ".mvn\wrapper" mkdir ".mvn\wrapper"
powershell -Command "Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/takari/maven-wrapper/master/maven-wrapper.properties' -OutFile '.mvn\wrapper\maven-wrapper.properties'"

REM Download .mvn/wrapper/maven-wrapper.jar
powershell -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/io/takari/maven-wrapper/0.5.6/maven-wrapper-0.5.6.jar' -OutFile '.mvn\wrapper\maven-wrapper.jar'"

echo.
echo ========================================
echo Maven Wrapper created successfully!
echo Now you can use: mvnw.cmd spring-boot:run
echo ========================================
pause 