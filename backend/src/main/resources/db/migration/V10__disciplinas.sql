CREATE TABLE IF NOT EXISTS disciplinas (
    id           CHAR(36)     NOT NULL,
    tenant_id    CHAR(36)     NOT NULL,
    nome         VARCHAR(120) NOT NULL,
    codigo       VARCHAR(20),
    carga_horaria INT,
    descricao    TEXT,
    ativa        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    created_by   CHAR(36),
    updated_by   CHAR(36),
    deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

CREATE INDEX idx_disciplinas_tenant ON disciplinas(tenant_id);
CREATE INDEX idx_disciplinas_nome   ON disciplinas(tenant_id, nome);
