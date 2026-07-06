-- ponytail: dev/staging базы могли получить колонку раньше вне Flyway; делаем миграцию идемпотентной.
ALTER TABLE organization_unit ADD COLUMN IF NOT EXISTS level INTEGER DEFAULT 1;

-- Обновляем существующие записи, устанавливая уровень по умолчанию
UPDATE organization_unit SET level = 1 WHERE level IS NULL;
