-- Реестр MAC-адресов Wi-Fi модулей WT2 и справочник типов оборудования.
-- ponytail: PascalCase + кавычки — как JPA @Table(name = "MacAddressRegistry"); FK на WeldingMachine через discovery.

CREATE TABLE IF NOT EXISTS "MacEquipmentType" (
    "ID"   SERIAL PRIMARY KEY,
    "Name" VARCHAR(128) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS "MacAddressRegistry" (
    "ID"                 SERIAL PRIMARY KEY,
    "MAC"                VARCHAR(12)  NOT NULL UNIQUE,
    "MacEquipmentTypeID" INTEGER      NOT NULL REFERENCES "MacEquipmentType" ("ID"),
    "Status"             VARCHAR(16)  NOT NULL,
    "DateCreated"        TIMESTAMP    NOT NULL DEFAULT NOW(),
    "EnteredByName"      VARCHAR(256) NOT NULL,
    "SessionCount"       BIGINT       NOT NULL DEFAULT 0,
    "LastPacketAt"       TIMESTAMP    NULL,
    "WeldingMachineID"   INTEGER      NULL UNIQUE
);

CREATE INDEX IF NOT EXISTS idx_mac_address_registry_status
    ON "MacAddressRegistry" ("Status");
CREATE INDEX IF NOT EXISTS idx_mac_address_registry_date_created
    ON "MacAddressRegistry" ("DateCreated");
CREATE INDEX IF NOT EXISTS idx_mac_address_registry_type
    ON "MacAddressRegistry" ("MacEquipmentTypeID");

INSERT INTO "MacEquipmentType" ("Name")
VALUES ('Аппарат'), ('Роутер')
ON CONFLICT ("Name") DO NOTHING;

DO $migration$
DECLARE
    machine_oid oid;
    machine_ref text;
    machine_id_col text;
    machine_mac_col text;
    machine_status_col text;
    machine_date_col text;
    apparatus_type_id INTEGER;
    rec RECORD;
    norm_mac TEXT;
    fk_name TEXT := 'fk_mac_address_registry_welding_machine';
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

    IF machine_oid IS NOT NULL THEN
        SELECT a.attname INTO machine_id_col
        FROM pg_attribute a
        WHERE a.attrelid = machine_oid AND a.attnum > 0 AND NOT a.attisdropped
          AND lower(a.attname) = 'id'
        LIMIT 1;

        SELECT a.attname INTO machine_mac_col
        FROM pg_attribute a
        WHERE a.attrelid = machine_oid AND a.attnum > 0 AND NOT a.attisdropped
          AND lower(a.attname) = 'mac'
        LIMIT 1;

        SELECT a.attname INTO machine_status_col
        FROM pg_attribute a
        WHERE a.attrelid = machine_oid AND a.attnum > 0 AND NOT a.attisdropped
          AND lower(a.attname) = 'status'
        LIMIT 1;

        SELECT a.attname INTO machine_date_col
        FROM pg_attribute a
        WHERE a.attrelid = machine_oid AND a.attnum > 0 AND NOT a.attisdropped
          AND lower(a.attname) IN ('datecreated', 'date_created')
        LIMIT 1;

        IF machine_id_col IS NOT NULL
           AND to_regclass('"MacAddressRegistry"') IS NOT NULL
           AND NOT EXISTS (
               SELECT 1 FROM pg_constraint
               WHERE conname = fk_name
                 AND conrelid = to_regclass('"MacAddressRegistry"')
           ) THEN
            EXECUTE format(
                'ALTER TABLE "MacAddressRegistry" ADD CONSTRAINT %I '
                || 'FOREIGN KEY ("WeldingMachineID") REFERENCES %s (%I)',
                fk_name, machine_ref, machine_id_col
            );
        END IF;
    ELSE
        RAISE NOTICE 'V1_26: welding machine table not found, skip FK and data migration';
        RETURN;
    END IF;

    SELECT "ID" INTO apparatus_type_id FROM "MacEquipmentType" WHERE "Name" = 'Аппарат' LIMIT 1;
    IF apparatus_type_id IS NULL THEN
        RAISE NOTICE 'V1_26: MacEquipmentType Аппарат not found, skip data migration';
        RETURN;
    END IF;

    FOR rec IN EXECUTE format(
        'SELECT wm.%I AS id, wm.%I AS mac, wm.%I AS status '
        || 'FROM %s wm '
        || 'WHERE wm.%I IS NOT NULL AND btrim(wm.%I::text) <> %L',
        machine_id_col, machine_mac_col, machine_status_col,
        machine_ref, machine_mac_col, machine_mac_col, ''
    ) LOOP
        IF rec.status IN (2, 5) THEN
            CONTINUE;
        END IF;

        norm_mac := upper(regexp_replace(rec.mac, '[^0-9A-Fa-f]', '', 'g'));
        IF length(norm_mac) <> 12 OR norm_mac = 'XXXXXXXXXXXX' THEN
            CONTINUE;
        END IF;
        IF upper(rec.mac) LIKE 'DELETED_%' THEN
            CONTINUE;
        END IF;

        IF machine_date_col IS NOT NULL THEN
            EXECUTE format(
                'INSERT INTO "MacAddressRegistry" '
                || '("MAC", "MacEquipmentTypeID", "Status", "DateCreated", "EnteredByName", '
                || '"SessionCount", "WeldingMachineID") '
                || 'SELECT $1, $2, ''ACTIVE'', COALESCE(wm.%I, NOW()), ''Администратор'', 0, wm.%I '
                || 'FROM %s wm WHERE wm.%I = $3 '
                || 'ON CONFLICT ("MAC") DO NOTHING',
                machine_date_col, machine_id_col, machine_ref, machine_id_col
            ) USING norm_mac, apparatus_type_id, rec.id;
        ELSE
            INSERT INTO "MacAddressRegistry" (
                "MAC", "MacEquipmentTypeID", "Status", "DateCreated", "EnteredByName",
                "SessionCount", "WeldingMachineID"
            )
            VALUES (norm_mac, apparatus_type_id, 'ACTIVE', NOW(), 'Администратор', 0, rec.id)
            ON CONFLICT ("MAC") DO NOTHING;
        END IF;
    END LOOP;
END $migration$;
