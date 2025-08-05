#!/bin/bash

echo "Starting WeldTelecom Application..."
echo "Backend: http://YOUR_IP:8083/api"
echo "Frontend: http://YOUR_IP:3001"
echo ""

# Запускаем backend в фоне
echo "Starting Backend..."
cd WT2
./mvnw spring-boot:run &
BACKEND_PID=$!

# Ждем немного для запуска backend
sleep 30

# Запускаем frontend
echo "Starting Frontend..."
cd ../BetaFrontWT
npm run start-external &
FRONTEND_PID=$!

echo "Application started!"
echo "Backend PID: $BACKEND_PID"
echo "Frontend PID: $FRONTEND_PID"
echo ""
echo "To stop the application, run: kill $BACKEND_PID $FRONTEND_PID"

# Ждем завершения
wait 