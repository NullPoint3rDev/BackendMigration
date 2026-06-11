ALTER TABLE welding_machine_daily_stats
    ADD COLUMN IF NOT EXISTS gas_consumption_l NUMERIC(16, 3) NOT NULL DEFAULT 0;

ALTER TABLE welding_machine_daily_stats
    ADD COLUMN IF NOT EXISTS gas_baseline_at_day_start_l NUMERIC(16, 3);
