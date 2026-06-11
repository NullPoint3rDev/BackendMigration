-- Staging: таблица user_account (snake_case), не "UserAccount".
DO $migration$
BEGIN
    IF to_regclass('public.user_account') IS NOT NULL THEN
        ALTER TABLE user_account ADD COLUMN IF NOT EXISTS allowed_user_actions TEXT;
    ELSIF to_regclass('public."UserAccount"') IS NOT NULL THEN
        ALTER TABLE "UserAccount" ADD COLUMN IF NOT EXISTS "AllowedUserActions" TEXT;
    ELSIF to_regclass('public.useraccount') IS NOT NULL THEN
        ALTER TABLE useraccount ADD COLUMN IF NOT EXISTS alloweduseractions TEXT;
    END IF;
END $migration$;
