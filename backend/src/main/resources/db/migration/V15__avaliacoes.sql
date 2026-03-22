CREATE TABLE IF NOT EXISTS avaliacoes (
    id            CHAR(36)       NOT NULL,
    tenant_id     CHAR(36)       NOT NULL,
    turma_id      CHAR(36)       NOT NULL,
    disciplina_id CHAR(36)       NOT NULL,
    nome          VARCHAR(120)   NOT NULL,
    tipo          VARCHAR(40)    NOT NULL DEFAULT 'prova',
    peso          DECIMAL(5,2)   NOT NULL DEFAULT 1.00,
    data_avaliacao DATE,
    nota_maxima   DECIMAL(5,2)   NOT NULL DEFAULT 10.00,
    created_at    DATETIME(6)    NOT NULL,
    updated_at    DATETIME(6)    NOT NULL,
    created_by    CHAR(36),
    updated_by    CHAR(36),
    deleted       BOOLEAN        NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

CREATE INDEX idx_avaliacoes_tenant ON avaliacoes(tenant_id);
CREATE INDEX idx_avaliacoes_turma  ON avaliacoes(turma_id);
