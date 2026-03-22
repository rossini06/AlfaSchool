CREATE TABLE IF NOT EXISTS professor_turma_disciplina (
    id            CHAR(36)    NOT NULL,
    tenant_id     CHAR(36)    NOT NULL,
    professor_id  CHAR(36)    NOT NULL,
    turma_id      CHAR(36)    NOT NULL,
    disciplina_id CHAR(36)    NOT NULL,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    created_by    CHAR(36),
    updated_by    CHAR(36),
    deleted       BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_ptd UNIQUE (professor_id, turma_id, disciplina_id, tenant_id)
);

CREATE INDEX idx_ptd_tenant    ON professor_turma_disciplina(tenant_id);
CREATE INDEX idx_ptd_turma     ON professor_turma_disciplina(turma_id);
CREATE INDEX idx_ptd_professor ON professor_turma_disciplina(professor_id);
