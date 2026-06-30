-- После исправления расчёта газа по накопительному счётчику: сбросить устаревший кэш.
-- computed_at в прошлое → shouldScheduleRecompute + синхронный пересчёт при первом запросе за сегодня.
UPDATE welding_machine_daily_stats
SET computed_at = TIMESTAMP '1970-01-01 00:00:00'
WHERE stat_date >= CURRENT_DATE - INTERVAL '30 days';
