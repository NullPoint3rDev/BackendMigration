-- Расширяем поля таблицы сварочных аппаратов (имя таблицы на staging может отличаться).
-- ponytail: ищем по колонке mac, а не по имени "WeldingMachine".
DO $migration$
DECLARE
    machine_oid oid;
    machine_ref text;
    col_name text;
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
        RAISE NOTICE 'V1_16: welding machine table not found, skip';
        RETURN;
    END IF;

    SELECT a.attname
    INTO col_name
    FROM pg_attribute a
    WHERE a.attrelid = machine_oid
      AND a.attnum > 0
      AND NOT a.attisdropped
      AND lower(a.attname) = 'modules'
    LIMIT 1;

    IF col_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE %s ALTER COLUMN %I TYPE TEXT', machine_ref, col_name);
    ELSE
        EXECUTE format('ALTER TABLE %s ADD COLUMN IF NOT EXISTS %I TEXT', machine_ref, 'Modules');
    END IF;

    SELECT a.attname
    INTO col_name
    FROM pg_attribute a
    WHERE a.attrelid = machine_oid
      AND a.attnum > 0
      AND NOT a.attisdropped
      AND lower(a.attname) IN ('serialnumber', 'serial_number')
    LIMIT 1;

    IF col_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE %s ALTER COLUMN %I TYPE VARCHAR(64)', machine_ref, col_name);
    END IF;

    SELECT a.attname
    INTO col_name
    FROM pg_attribute a
    WHERE a.attrelid = machine_oid
      AND a.attnum > 0
      AND NOT a.attisdropped
      AND lower(a.attname) IN ('inventorynumber', 'inventory_number')
    LIMIT 1;

    IF col_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE %s ALTER COLUMN %I TYPE VARCHAR(64)', machine_ref, col_name);
    END IF;
END $migration$;
