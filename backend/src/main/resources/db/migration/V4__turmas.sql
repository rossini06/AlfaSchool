CREATE TABLE IF NOT EXISTS turmas (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    unit_id CHAR(36),
    curso_id CHAR(36) NOT NULL,
    nome VARCHAR(80) NOT NULL,
    codigo VARCHAR(20),
    ano_letivo INT NOT NULL,
    turno VARCHAR(20) NOT NULL DEFAULT 'manha',
    professor_responsavel VARCHAR(120),
    capacidade_maxima INT NOT NULL DEFAULT 40,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by CHAR(36),
    updated_by CHAR(36),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_turmas_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    CONSTRAINT fk_turmas_unit FOREIGN KEY (unit_id) REFERENCES units(id),
    CONSTRAINT fk_turmas_curso FOREIGN KEY (curso_id) REFERENCES cursos(id)
);
CREATE INDEX idx_turmas_tenant ON turmas(tenant_id);
CREATE INDEX idx_turmas_curso ON turmas(curso_id);
