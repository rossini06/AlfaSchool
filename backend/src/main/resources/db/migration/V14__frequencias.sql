CREATE TABLE IF NOT EXISTS frequencias (
    id            CHAR(36)    NOT NULL,
    tenant_id     CHAR(36)    NOT NULL,
    aluno_id      CHAR(36)    NOT NULL,
    turma_id      CHAR(36)    NOT NULL,
    disciplina_id CHAR(36)    NOT NULL,
    data          DATE        NOT NULL,
    presente      BOOLEAN     NOT NULL DEFAULT TRUE,
    obs           VARCHAR(255),
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    created_by    CHAR(36),
    updated_by    CHAR(36),
    deleted       BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_frequencia UNIQUE (aluno_id, turma_id, disciplina_id, data, tenant_id)
);

CREATE INDEX idx_frequencias_tenant ON frequencias(tenant_id);
CREATE INDEX idx_frequencias_aluno  ON frequencias(aluno_id);
CREATE INDEX idx_frequencias_turma  ON frequencias(turma_id);
CREATE INDEX idx_frequencias_data   ON frequencias(data);
