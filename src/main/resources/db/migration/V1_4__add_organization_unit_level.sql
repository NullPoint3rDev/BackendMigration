-- Добавляем столбец level в таблицу organization_unit
ALTER TABLE organization_unit ADD COLUMN level INTEGER DEFAULT 1;

-- Обновляем существующие записи, устанавливая уровень по умолчанию
UPDATE organization_unit SET level = 1 WHERE level IS NULL;
