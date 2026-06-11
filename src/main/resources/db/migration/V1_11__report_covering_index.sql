-- Covering index для loadStatesForReport: Index Only Scan без лишних heap reads.
CREATE INDEX IF NOT EXISTS idx_wms_machine_date_report
ON welding_machine_state (welding_machineid, date_created)
INCLUDE (state_duration_ms, welding_machine_status);

ANALYZE welding_machine_state;
