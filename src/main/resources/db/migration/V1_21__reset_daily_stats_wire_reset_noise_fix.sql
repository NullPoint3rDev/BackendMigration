-- Расчёт проволоки: мелкие V-просадки счётчика больше не считаются сбросом (давали сотни кг).
-- Сбрасываем кэш, чтобы строки, пересчитанные предыдущей версией, перезаписались корректно.
UPDATE welding_machine_daily_stats
SET computed_at = TIMESTAMP '1970-01-01 00:00:00'
WHERE stat_date >= CURRENT_DATE - INTERVAL '30 days';
