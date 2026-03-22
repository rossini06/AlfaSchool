CREATE TABLE IF NOT EXISTS responsaveis (
    id         CHAR(36)     NOT NULL,
    tenant_id  CHAR(36)     NOT NULL,
    aluno_id   CHAR(36)     NOT NULL,
    nome       VARCHAR(120) NOT NULL,
    cpf        VARCHAR(14),
    telefone   VARCHAR(20),
    email      VARCHAR(160),
    tipo       VARCHAR(20)  NOT NULL DEFAULT 'financeiro',
    principal  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    created_by CHAR(36),
    updated_by CHAR(36),
    deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

CREATE INDEX idx_responsaveis_tenant ON responsaveis(tenant_id);
CREATE INDEX idx_responsaveis_aluno  ON responsaveis(aluno_id);
