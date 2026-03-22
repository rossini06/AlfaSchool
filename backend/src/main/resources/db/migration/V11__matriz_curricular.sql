CREATE TABLE IF NOT EXISTS matriz_curricular (
    id            CHAR(36)    NOT NULL,
    tenant_id     CHAR(36)    NOT NULL,
    curso_id      CHAR(36)    NOT NULL,
    disciplina_id CHAR(36)    NOT NULL,
    periodo       VARCHAR(60) NOT NULL,
    carga_horaria INT,
    obrigatoria   BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    created_by    CHAR(36),
    updated_by    CHAR(36),
    deleted       BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_matriz_curso_disc_periodo UNIQUE (curso_id, disciplina_id, periodo, tenant_id)
);

CREATE INDEX idx_matriz_tenant ON matriz_curricular(tenant_id);
CREATE INDEX idx_matriz_curso  ON matriz_curricular(curso_id);
