-- Разовая синхронизация rfidEnabled ↔ modules.options.rfid:
-- если хоть одно false → оба false.
DO $migration$
DECLARE
    machine_oid oid;
    machine_ref text;
    rfid_col text;
    modules_col text;
    rec record;
    mods jsonb;
    opt_rfid boolean;
    col_rfid boolean;
    new_mods text;
BEGIN
    SELECT c.oid, format('%I.%I', n.nspname, c.relname)
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
        RAISE NOTICE 'V1_25: welding machine table not found, skip';
        RETURN;
    END IF;

    SELECT a.attname INTO rfid_col
    FROM pg_attribute a
    WHERE a.attrelid = machine_oid AND a.attnum > 0 AND NOT a.attisdropped
      AND lower(a.attname) IN ('rfidenabled', 'rfid_enabled')
    LIMIT 1;

    SELECT a.attname INTO modules_col
    FROM pg_attribute a
    WHERE a.attrelid = machine_oid AND a.attnum > 0 AND NOT a.attisdropped
      AND lower(a.attname) IN ('modules')
    LIMIT 1;

    IF rfid_col IS NULL OR modules_col IS NULL THEN
        RAISE NOTICE 'V1_25: rfid/modules columns missing, skip';
        RETURN;
    END IF;

    FOR rec IN EXECUTE format(
        'SELECT id, %I AS rfid_enabled, %I AS modules FROM %s',
        rfid_col, modules_col, machine_ref
    ) LOOP
        col_rfid := COALESCE(rec.rfid_enabled, TRUE);
        opt_rfid := TRUE;
        BEGIN
            IF rec.modules IS NOT NULL AND btrim(rec.modules) <> '' THEN
                mods := rec.modules::jsonb;
                IF mods ? 'options' AND (mods -> 'options') ? 'rfid' THEN
                    opt_rfid := COALESCE((mods -> 'options' ->> 'rfid')::boolean, TRUE);
                END IF;
            ELSE
                mods := '{}'::jsonb;
            END IF;
        EXCEPTION WHEN others THEN
            mods := '{}'::jsonb;
            opt_rfid := TRUE;
        END;

        IF col_rfid AND opt_rfid THEN
            CONTINUE;
        END IF;

        -- хотя бы одно false → оба false
        IF mods ? 'options' THEN
            mods := jsonb_set(mods, '{options,rfid}', 'false'::jsonb, TRUE);
        ELSE
            mods := jsonb_set(mods, '{options}', '{"rfid": false}'::jsonb, TRUE);
        END IF;
        new_mods := mods::text;

        EXECUTE format(
            'UPDATE %s SET %I = FALSE, %I = $1 WHERE id = $2',
            machine_ref, rfid_col, modules_col
        ) USING new_mods, rec.id;
    END LOOP;
END $migration$;
