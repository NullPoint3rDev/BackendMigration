-- Расширяем поля WeldingMachine под JSON modules и длинные номера.
ALTER TABLE "WeldingMachine"
    ALTER COLUMN "Modules" TYPE TEXT;

ALTER TABLE "WeldingMachine"
    ALTER COLUMN "SerialNumber" TYPE VARCHAR(64);

ALTER TABLE "WeldingMachine"
    ALTER COLUMN "InventoryNumber" TYPE VARCHAR(64);
