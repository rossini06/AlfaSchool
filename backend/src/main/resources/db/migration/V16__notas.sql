CREATE TABLE IF NOT EXISTS notas (
    id           CHAR(36)     NOT NULL,
    tenant_id    CHAR(36)     NOT NULL,
    aluno_id     CHAR(36)     NOT NULL,
    avaliacao_id CHAR(36)     NOT NULL,
    nota         DECIMAL(5,2),
    obs          VARCHAR(255),
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    created_by   CHAR(36),
    updated_by   CHAR(36),
    deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_nota_aluno_avaliacao UNIQUE (aluno_id, avaliacao_id, tenant_id)
);

CREATE INDEX idx_notas_tenant    ON notas(tenant_id);
CREATE INDEX idx_notas_aluno     ON notas(aluno_id);
CREATE INDEX idx_notas_avaliacao ON notas(avaliacao_id);
