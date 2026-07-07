-- Новые поля аппарата: RfidEnabled (активация RFID), ManufactureDate (дата изготовления),
-- MaintenanceIntervalUnit (единица наработки между ТО: HOURS|DAYS).
-- ponytail: та же table-discovery, что в V1_16/V1_22 — staging может быть snake_case или PascalCase.
DO $migration$
DECLARE
    machine_oid oid;
    machine_ref text;
    ref_col text;
    snake boolean;
BEGIN
    SELECT c.oid,
           format('%I.%I', n.nspname, c.relname)
    INTO machine_oid, machine_ref
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = current_schema()
      AND c.relkind = 'r'
      AND lower(c.relname) LIKE '%welding%machine%'
      AND lower(c.relname) NOT LIKE '%state%'
      AND lower(c.relname) NOT LIKE '%parameter%'
      AND lower(c.relname) NOT LIKE '%type%'
      AND lower(c.relname) NOT LIKE '%segment%'
      AND lower(c.relname) NOT LIKE '%stats%'
      AND lower(c.relname) NOT LIKE '%daily%'
      AND EXISTS (
          SELECT 1 FROM pg_attribute a
          WHERE a.attrelid = c.oid AND a.attnum > 0 AND NOT a.attisdropped
            AND lower(a.attname) IN ('mac', 'serialnumber', 'serial_number')
      )
    ORDER BY CASE
        WHEN c.relname = 'WeldingMachine' THEN 0
        WHEN lower(c.relname) = 'weldingmachine' THEN 1
        WHEN lower(c.relname) = 'welding_machine' THEN 2
        ELSE 3
    END
    LIMIT 1;

    IF machine_oid IS NULL THEN
        RAISE NOTICE 'V1_23: welding machine table not found, skip';
        RETURN;
    END IF;

    -- Определяем стиль именования колонок (snake_case vs PascalCase) по эталонной колонке.
    SELECT a.attname
    INTO ref_col
    FROM pg_attribute a
    WHERE a.attrelid = machine_oid
      AND a.attnum > 0
      AND NOT a.attisdropped
      AND lower(a.attname) IN ('lastonlineon', 'last_online_on', 'lastpoweredonat', 'last_powered_on_at', 'serialnumber', 'serial_number')
    LIMIT 1;

    snake := (ref_col IS NOT NULL AND position('_' in ref_col) > 0);

    -- RfidEnabled BOOLEAN, существующие аппараты остаются активными (TRUE).
    IF NOT EXISTS (
        SELECT 1 FROM pg_attribute a
        WHERE a.attrelid = machine_oid AND a.attnum > 0 AND NOT a.attisdropped
          AND lower(a.attname) IN ('rfidenabled', 'rfid_enabled')
    ) THEN
        EXECUTE format('ALTER TABLE %s ADD COLUMN %I BOOLEAN NOT NULL DEFAULT TRUE',
                       machine_ref, CASE WHEN snake THEN 'rfid_enabled' ELSE 'RfidEnabled' END);
    END IF;

    -- ManufactureDate DATE.
    IF NOT EXISTS (
        SELECT 1 FROM pg_attribute a
        WHERE a.attrelid = machine_oid AND a.attnum > 0 AND NOT a.attisdropped
          AND lower(a.attname) IN ('manufacturedate', 'manufacture_date')
    ) THEN
        EXECUTE format('ALTER TABLE %s ADD COLUMN %I DATE',
                       machine_ref, CASE WHEN snake THEN 'manufacture_date' ELSE 'ManufactureDate' END);
    END IF;

    -- MaintenanceIntervalUnit VARCHAR(16): 'HOURS' | 'DAYS'.
    IF NOT EXISTS (
        SELECT 1 FROM pg_attribute a
        WHERE a.attrelid = machine_oid AND a.attnum > 0 AND NOT a.attisdropped
          AND lower(a.attname) IN ('maintenanceintervalunit', 'maintenance_interval_unit')
    ) THEN
        EXECUTE format('ALTER TABLE %s ADD COLUMN %I VARCHAR(16)',
                       machine_ref, CASE WHEN snake THEN 'maintenance_interval_unit' ELSE 'MaintenanceIntervalUnit' END);
    END IF;
END $migration$;
