-- Реестр MAC-адресов Wi-Fi модулей WT2 и справочник типов оборудования.

CREATE TABLE IF NOT EXISTS MacEquipmentType (
    ID          SERIAL PRIMARY KEY,
    Name        VARCHAR(128) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS MacAddressRegistry (
    ID                  SERIAL PRIMARY KEY,
    MAC                 VARCHAR(12)  NOT NULL UNIQUE,
    MacEquipmentTypeID  INTEGER      NOT NULL REFERENCES MacEquipmentType (ID),
    Status              VARCHAR(16)  NOT NULL,
    DateCreated         TIMESTAMP    NOT NULL DEFAULT NOW(),
    EnteredByName       VARCHAR(256) NOT NULL,
    SessionCount        BIGINT       NOT NULL DEFAULT 0,
    LastPacketAt        TIMESTAMP    NULL,
    WeldingMachineID    INTEGER      NULL UNIQUE REFERENCES WeldingMachine (ID)
);

CREATE INDEX IF NOT EXISTS idx_mac_address_registry_status ON MacAddressRegistry (Status);
CREATE INDEX IF NOT EXISTS idx_mac_address_registry_date_created ON MacAddressRegistry (DateCreated);
CREATE INDEX IF NOT EXISTS idx_mac_address_registry_type ON MacAddressRegistry (MacEquipmentTypeID);

INSERT INTO MacEquipmentType (Name)
VALUES ('Аппарат'), ('Роутер')
ON CONFLICT (Name) DO NOTHING;

-- Миграция существующих MAC из WeldingMachine (активные, не Purging/Deleted).
DO $migration$
DECLARE
    apparatus_type_id INTEGER;
    rec RECORD;
    norm_mac TEXT;
BEGIN
    SELECT id INTO apparatus_type_id FROM MacEquipmentType WHERE Name = 'Аппарат' LIMIT 1;
    IF apparatus_type_id IS NULL THEN
        RAISE NOTICE 'V1_26: MacEquipmentType Аппарат not found, skip WeldingMachine migration';
        RETURN;
    END IF;

    FOR rec IN
        SELECT wm.id, wm.mac
        FROM WeldingMachine wm
        WHERE wm.mac IS NOT NULL
          AND btrim(wm.mac) <> ''
          AND wm.status NOT IN (2, 5) -- GeneralStatus: Deleted=2, Purging=5
          AND upper(wm.mac) NOT LIKE 'DELETED_%'
    LOOP
        norm_mac := upper(regexp_replace(rec.mac, '[^0-9A-Fa-f]', '', 'g'));
        IF length(norm_mac) <> 12 THEN
            CONTINUE;
        END IF;
        IF norm_mac = 'XXXXXXXXXXXX' THEN
            CONTINUE;
        END IF;

        INSERT INTO MacAddressRegistry (
            MAC, MacEquipmentTypeID, Status, DateCreated, EnteredByName,
            SessionCount, WeldingMachineID
        )
        VALUES (
            norm_mac,
            apparatus_type_id,
            'ACTIVE',
            COALESCE(
                (SELECT wm2."DateCreated" FROM WeldingMachine wm2 WHERE wm2.id = rec.id),
                NOW()
            ),
            'Администратор',
            0,
            rec.id
        )
        ON CONFLICT (MAC) DO NOTHING;
    END LOOP;
END $migration$;
