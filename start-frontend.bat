@echo off
echo Starting React Frontend...
echo Frontend will be available at: http://YOUR_IP:3001
echo.

cd BetaFrontWT
set REACT_APP_API_URL=http://192.168.0.100:8084/api
set HOST=0.0.0.0
set PORT=3001
call npm run start
pause 