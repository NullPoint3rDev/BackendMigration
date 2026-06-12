-- Staging baseline (1.10) пропустил V1_7/V1_8: колонка могла остаться varchar(255).
-- Полный набор allowedUserActions для админа предприятия ~400+ символов.
DO $migration$
BEGIN
    IF to_regclass('public.user_account') IS NOT NULL THEN
        ALTER TABLE user_account ADD COLUMN IF NOT EXISTS allowed_user_actions TEXT;
        ALTER TABLE user_account ALTER COLUMN allowed_user_actions TYPE TEXT;
    ELSIF to_regclass('public."UserAccount"') IS NOT NULL THEN
        ALTER TABLE "UserAccount" ADD COLUMN IF NOT EXISTS "AllowedUserActions" TEXT;
        ALTER TABLE "UserAccount" ALTER COLUMN "AllowedUserActions" TYPE TEXT;
    ELSIF to_regclass('public.useraccount') IS NOT NULL THEN
        ALTER TABLE useraccount ADD COLUMN IF NOT EXISTS alloweduseractions TEXT;
        ALTER TABLE useraccount ALTER COLUMN alloweduseractions TYPE TEXT;
    END IF;
END $migration$;
