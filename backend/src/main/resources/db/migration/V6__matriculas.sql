CREATE TABLE IF NOT EXISTS matriculas (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    unit_id CHAR(36),
    aluno_id CHAR(36) NOT NULL,
    turma_id CHAR(36) NOT NULL,
    numero_matricula VARCHAR(30) NOT NULL,
    data_matricula DATE NOT NULL,
    data_conclusao DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ativa',
    obs TEXT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by CHAR(36),
    updated_by CHAR(36),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_matriculas_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    CONSTRAINT fk_matriculas_aluno FOREIGN KEY (aluno_id) REFERENCES alunos(id),
    CONSTRAINT fk_matriculas_turma FOREIGN KEY (turma_id) REFERENCES turmas(id),
    CONSTRAINT uk_matriculas_numero UNIQUE (numero_matricula, tenant_id)
);
CREATE INDEX idx_matriculas_tenant ON matriculas(tenant_id);
CREATE INDEX idx_matriculas_aluno ON matriculas(aluno_id);
CREATE INDEX idx_matriculas_turma ON matriculas(turma_id);
CREATE INDEX idx_matriculas_status ON matriculas(status);
