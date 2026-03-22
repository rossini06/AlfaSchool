-- Extend professores with date fields
ALTER TABLE professores
    ADD COLUMN data_nascimento  DATE NULL,
    ADD COLUMN data_contratacao DATE NULL;
