#!/bin/bash

EXTERNAL_IP="5.227.31.124"
LOCAL_IP="192.168.0.100"

echo "🔍 Проверка доступности WeldTelecom приложения"
echo "================================================"
echo "Внешний IP: $EXTERNAL_IP"
echo "Локальный IP: $LOCAL_IP"
echo ""

# Проверка локального доступа
echo "📱 Проверка локального доступа:"
echo "--------------------------------"

echo -n "Backend (локально): "
if curl -s http://$LOCAL_IP:8083/api > /dev/null 2>&1; then
    echo "✅ Доступен"
else
    echo "❌ Недоступен"
fi

echo -n "Frontend (локально): "
if curl -s http://$LOCAL_IP:3001 > /dev/null 2>&1; then
    echo "✅ Доступен"
else
    echo "❌ Недоступен"
fi

echo ""

# Проверка внешнего доступа
echo "🌐 Проверка внешнего доступа:"
echo "-----------------------------"

echo -n "Backend (внешне): "
if curl -s --connect-timeout 5 http://$EXTERNAL_IP:8083/api > /dev/null 2>&1; then
    echo "✅ Доступен"
else
    echo "❌ Недоступен (возможно, не настроен проброс портов)"
fi

echo -n "Frontend (внешне): "
if curl -s --connect-timeout 5 http://$EXTERNAL_IP:3001 > /dev/null 2>&1; then
    echo "✅ Доступен"
else
    echo "❌ Недоступен (возможно, не настроен проброс портов)"
fi

echo ""

# Проверка текущего внешнего IP
echo "🌍 Текущий внешний IP:"
echo "----------------------"
CURRENT_IP=$(curl -s ifconfig.me)
echo "Текущий: $CURRENT_IP"
echo "Ожидаемый: $EXTERNAL_IP"

if [ "$CURRENT_IP" = "$EXTERNAL_IP" ]; then
    echo "✅ IP совпадает"
else
    echo "⚠️  IP изменился! Обновите настройки."
fi

echo ""

# Инструкции
echo "📋 Следующие шаги:"
echo "=================="
echo "1. Если локальный доступ ❌ - запустите приложение:"
echo "   ./start-all.sh"
echo ""
echo "2. Если внешний доступ ❌ - настройте проброс портов:"
echo "   - Откройте настройки роутера (192.168.0.1)"
echo "   - Найдите 'Port Forwarding'"
echo "   - Добавьте правила для портов 8083 и 3001"
echo "   - Подробности в файле PORT_FORWARDING_GUIDE.md"
echo ""
echo "3. После настройки проверьте доступность:"
echo "   Frontend: http://$EXTERNAL_IP:3001"
echo "   Backend:  http://$EXTERNAL_IP:8083/api" 