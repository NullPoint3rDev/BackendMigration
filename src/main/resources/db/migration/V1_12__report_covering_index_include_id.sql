-- Дополняем covering index для loadStatesForReport:
-- добавляем INCLUDE (id), чтобы увеличить шанс index-only scan и уменьшить heap fetch.
CREATE INDEX IF NOT EXISTS idx_wms_machine_date_report_include_id
ON welding_machine_state (welding_machineid, date_created)
INCLUDE (id, state_duration_ms, welding_machine_status);

ANALYZE welding_machine_state;

