-- Только если V1_13 создала baseline как BIGINT (сырые десятые); приводим к литрам.
DO $migration$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns c
        WHERE c.table_schema = current_schema()
          AND c.table_name = 'welding_machine_daily_stats'
          AND c.column_name = 'gas_baseline_at_day_start_l'
          AND c.data_type = 'bigint'
    ) THEN
        EXECUTE '
            ALTER TABLE welding_machine_daily_stats
                ALTER COLUMN gas_baseline_at_day_start_l TYPE NUMERIC(16, 3)
                USING (gas_baseline_at_day_start_l::numeric / 10.0)';
    END IF;
END $migration$;
