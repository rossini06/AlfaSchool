CREATE TABLE IF NOT EXISTS professores (
    id           CHAR(36)     NOT NULL,
    tenant_id    CHAR(36)     NOT NULL,
    unit_id      CHAR(36),
    nome         VARCHAR(120) NOT NULL,
    cpf          VARCHAR(14),
    email        VARCHAR(160),
    telefone     VARCHAR(20),
    especialidade VARCHAR(120),
    status       VARCHAR(20)  NOT NULL DEFAULT 'ativo',
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    created_by   CHAR(36),
    updated_by   CHAR(36),
    deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_professor_cpf_tenant UNIQUE (cpf, tenant_id)
);

CREATE INDEX idx_professores_tenant ON professores(tenant_id);
CREATE INDEX idx_professores_nome   ON professores(tenant_id, nome);
