-- Расход проволоки теперь считается по дельтам накопительного счётчика (метры → кг),
-- а не «скорость × время» (давало абсурдные десятки кг). Сбрасываем кэш для пересчёта.
UPDATE welding_machine_daily_stats
SET computed_at = TIMESTAMP '1970-01-01 00:00:00'
WHERE stat_date >= CURRENT_DATE - INTERVAL '30 days';
