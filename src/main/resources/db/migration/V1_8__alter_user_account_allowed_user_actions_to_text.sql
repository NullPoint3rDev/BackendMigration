-- Some environments already had "AllowedUserActions" created as VARCHAR(255)
-- (e.g., via Hibernate auto-ddl / manual DDL). V1_7 only ADDs the column if missing,
-- so it won't fix existing VARCHAR columns. This migration normalizes the type to TEXT.

ALTER TABLE "UserAccount"
    ADD COLUMN IF NOT EXISTS "AllowedUserActions" TEXT;

ALTER TABLE "UserAccount"
    ALTER COLUMN "AllowedUserActions" TYPE TEXT;

