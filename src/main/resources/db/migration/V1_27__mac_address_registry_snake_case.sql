-- ponytail: Spring Boot PhysicalNamingStrategy maps @Table("MacAddressRegistry") → mac_address_registry.
-- V1_26 created quoted PascalCase tables; Hibernate queries snake_case — rename to match.

DO $migration$
BEGIN
    IF to_regclass('"MacEquipmentType"') IS NOT NULL THEN
        ALTER TABLE "MacEquipmentType" RENAME TO mac_equipment_type;
        ALTER TABLE mac_equipment_type RENAME COLUMN "ID" TO id;
        ALTER TABLE mac_equipment_type RENAME COLUMN "Name" TO name;
    END IF;

    IF to_regclass('"MacAddressRegistry"') IS NOT NULL THEN
        ALTER TABLE "MacAddressRegistry" RENAME TO mac_address_registry;
        ALTER TABLE mac_address_registry RENAME COLUMN "ID" TO id;
        ALTER TABLE mac_address_registry RENAME COLUMN "MAC" TO mac;
        ALTER TABLE mac_address_registry RENAME COLUMN "MacEquipmentTypeID" TO mac_equipment_typeid;
        ALTER TABLE mac_address_registry RENAME COLUMN "Status" TO status;
        ALTER TABLE mac_address_registry RENAME COLUMN "DateCreated" TO date_created;
        ALTER TABLE mac_address_registry RENAME COLUMN "EnteredByName" TO entered_by_name;
        ALTER TABLE mac_address_registry RENAME COLUMN "SessionCount" TO session_count;
        ALTER TABLE mac_address_registry RENAME COLUMN "LastPacketAt" TO last_packet_at;
        ALTER TABLE mac_address_registry RENAME COLUMN "WeldingMachineID" TO welding_machineid;
    END IF;
END $migration$;
