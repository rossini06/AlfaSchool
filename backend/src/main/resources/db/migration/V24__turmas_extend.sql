-- Extend turmas with date range and explicit status
ALTER TABLE turmas
    ADD COLUMN data_inicio DATE         NULL,
    ADD COLUMN data_fim    DATE         NULL,
    ADD COLUMN status      VARCHAR(20)  NULL DEFAULT 'planejada';
