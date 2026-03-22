CREATE TABLE IF NOT EXISTS cursos (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    unit_id CHAR(36),
    nome VARCHAR(160) NOT NULL,
    codigo VARCHAR(20),
    descricao TEXT,
    carga_horaria INT,
    modalidade VARCHAR(20) NOT NULL DEFAULT 'presencial',
    nivel VARCHAR(30),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by CHAR(36),
    updated_by CHAR(36),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_cursos_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    CONSTRAINT fk_cursos_unit FOREIGN KEY (unit_id) REFERENCES units(id)
);
CREATE INDEX idx_cursos_tenant ON cursos(tenant_id);
CREATE INDEX idx_cursos_unit ON cursos(unit_id);
