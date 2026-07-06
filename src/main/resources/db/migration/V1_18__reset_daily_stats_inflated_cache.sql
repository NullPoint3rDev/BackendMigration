-- Убрали монотонный Math.max: раздутые пики таймеров/проволоки больше не залипают.
-- Сбрасываем кэш, чтобы первый пересчёт перезаписал значения честным результатом (в т.ч. вниз).
UPDATE welding_machine_daily_stats
SET computed_at = TIMESTAMP '1970-01-01 00:00:00'
WHERE stat_date >= CURRENT_DATE - INTERVAL '2 days';
