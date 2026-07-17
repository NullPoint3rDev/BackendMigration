-- Одноразовая коррекция last_weld_at: до фикса WeldingDeviceManagerService писал
-- LocalDateTime.now() (JVM Europe/Moscow), а API/сериализатор трактуют naive TIMESTAMP как UTC (+03:00 при отдаче).
-- Вычитаем 3 часа у уже сохранённых значений, чтобы они совпали с реальным московским временем шва.
DO $migration$
DECLARE
    machine_oid oid;
    machine_ref text;
    weld_col text;
    updated_count int;
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
        RAISE NOTICE 'V1_28: welding machine table not found, skip';
        RETURN;
    END IF;

    SELECT a.attname
    INTO weld_col
    FROM pg_attribute a
    WHERE a.attrelid = machine_oid
      AND a.attnum > 0
      AND NOT a.attisdropped
      AND lower(a.attname) IN ('lastweldat', 'last_weld_at')
    LIMIT 1;

    IF weld_col IS NULL THEN
        RAISE NOTICE 'V1_28: last_weld_at column not found on %, skip', machine_ref;
        RETURN;
    END IF;

    EXECUTE format(
        'UPDATE %s SET %I = %I - INTERVAL ''3 hours'' WHERE %I IS NOT NULL',
        machine_ref, weld_col, weld_col, weld_col
    );
    GET DIAGNOSTICS updated_count = ROW_COUNT;
    RAISE NOTICE 'V1_28: corrected % row(s) on %.% (-3h)', updated_count, machine_ref, weld_col;
END $migration$;
