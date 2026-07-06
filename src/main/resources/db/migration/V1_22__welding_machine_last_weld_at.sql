-- Дата/время последней сварки на аппарате (WeldingMachine.lastWeldAt).
-- ponytail: та же table-discovery, что в V1_16 — staging может быть snake_case или PascalCase.
DO $migration$
DECLARE
    machine_oid oid;
    machine_ref text;
    ref_col text;
    new_col text;
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
        RAISE NOTICE 'V1_22: welding machine table not found, skip';
        RETURN;
    END IF;

    IF EXISTS (
        SELECT 1 FROM pg_attribute a
        WHERE a.attrelid = machine_oid
          AND a.attnum > 0
          AND NOT a.attisdropped
          AND lower(a.attname) IN ('lastweldat', 'last_weld_at')
    ) THEN
        RAISE NOTICE 'V1_22: last_weld_at already exists on %, skip', machine_ref;
        RETURN;
    END IF;

    SELECT a.attname
    INTO ref_col
    FROM pg_attribute a
    WHERE a.attrelid = machine_oid
      AND a.attnum > 0
      AND NOT a.attisdropped
      AND lower(a.attname) IN ('lastonlineon', 'last_online_on', 'lastpoweredonat', 'last_powered_on_at')
    LIMIT 1;

    IF ref_col IS NOT NULL AND position('_' in ref_col) > 0 THEN
        new_col := 'last_weld_at';
    ELSE
        new_col := 'LastWeldAt';
    END IF;

    EXECUTE format('ALTER TABLE %s ADD COLUMN %I TIMESTAMP', machine_ref, new_col);
END $migration$;
