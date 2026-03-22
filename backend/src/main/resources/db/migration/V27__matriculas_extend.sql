-- Extend matriculas with type, academic status, and discount
ALTER TABLE matriculas
    ADD COLUMN tipo             VARCHAR(30)  NULL DEFAULT 'regular',
    ADD COLUMN status_academico VARCHAR(30)  NULL DEFAULT 'cursando',
    ADD COLUMN desconto         DECIMAL(5,2) NULL DEFAULT 0.00;
