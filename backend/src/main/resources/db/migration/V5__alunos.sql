CREATE TABLE IF NOT EXISTS alunos (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    unit_id CHAR(36),
    nome VARCHAR(120) NOT NULL,
    cpf VARCHAR(14),
    rg VARCHAR(20),
    email VARCHAR(160),
    telefone VARCHAR(20),
    data_nascimento DATE,
    sexo VARCHAR(10),
    endereco VARCHAR(255),
    cidade VARCHAR(120),
    estado VARCHAR(2),
    cep VARCHAR(9),
    nome_responsavel VARCHAR(120),
    telefone_responsavel VARCHAR(20),
    email_responsavel VARCHAR(160),
    foto TEXT,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by CHAR(36),
    updated_by CHAR(36),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_alunos_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    CONSTRAINT fk_alunos_unit FOREIGN KEY (unit_id) REFERENCES units(id)
);
CREATE INDEX idx_alunos_tenant ON alunos(tenant_id);
CREATE INDEX idx_alunos_cpf ON alunos(cpf);
CREATE INDEX idx_alunos_nome ON alunos(nome);
