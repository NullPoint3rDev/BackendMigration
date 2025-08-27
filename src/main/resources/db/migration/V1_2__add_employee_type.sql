-- Добавляем столбец employee_type в таблицу Employees (без NOT NULL)
ALTER TABLE Employees ADD COLUMN employee_type VARCHAR(50);

-- Обновляем существующие записи, устанавливая тип по умолчанию
-- Администраторы
UPDATE Employees SET employee_type = 'ADMIN' WHERE user_role_id = 1;

-- Обычные пользователи
UPDATE Employees SET employee_type = 'PROGRAMMER' WHERE user_role_id = 2;
