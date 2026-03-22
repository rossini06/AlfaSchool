CREATE TABLE IF NOT EXISTS contratos (
    id             CHAR(36)    NOT NULL,
    tenant_id      CHAR(36)    NOT NULL,
    aluno_id       CHAR(36)    NOT NULL,
    responsavel_id CHAR(36),
    plano_id       CHAR(36)    NOT NULL,
    matricula_id   CHAR(36),
    data_inicio    DATE        NOT NULL,
    data_fim       DATE,
    status         VARCHAR(20) NOT NULL DEFAULT 'ativo',
    obs            TEXT,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    created_by     CHAR(36),
    updated_by     CHAR(36),
    deleted        BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

CREATE INDEX idx_contratos_tenant ON contratos(tenant_id);
CREATE INDEX idx_contratos_aluno  ON contratos(aluno_id);
