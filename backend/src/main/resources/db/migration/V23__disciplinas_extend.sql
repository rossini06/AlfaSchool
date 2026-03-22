-- Extend disciplinas with curriculum and evaluation fields
ALTER TABLE disciplinas
    ADD COLUMN curso_id            CHAR(36)      NULL,
    ADD COLUMN tipo                VARCHAR(30)   NULL,
    ADD COLUMN nota_maxima         DECIMAL(4,2)  NULL DEFAULT 10.00,
    ADD COLUMN peso                DECIMAL(4,2)  NULL DEFAULT 1.00,
    ADD COLUMN permite_recuperacao BOOLEAN       NULL DEFAULT TRUE,
    ADD COLUMN tipo_avaliacao      VARCHAR(30)   NULL;
