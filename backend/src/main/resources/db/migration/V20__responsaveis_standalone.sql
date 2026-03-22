-- Make aluno_id nullable for standalone responsáveis
ALTER TABLE responsaveis MODIFY COLUMN aluno_id CHAR(36) NULL;

-- Add new fields to responsaveis
ALTER TABLE responsaveis
  ADD COLUMN rg VARCHAR(20),
  ADD COLUMN data_nascimento DATE,
  ADD COLUMN sexo VARCHAR(10),
  ADD COLUMN estado_civil VARCHAR(20),
  ADD COLUMN profissao VARCHAR(100),
  ADD COLUMN empresa VARCHAR(120),
  ADD COLUMN logradouro VARCHAR(255),
  ADD COLUMN numero_endereco VARCHAR(20),
  ADD COLUMN complemento VARCHAR(100),
  ADD COLUMN bairro VARCHAR(100),
  ADD COLUMN cidade VARCHAR(120),
  ADD COLUMN estado VARCHAR(2),
  ADD COLUMN cep VARCHAR(9),
  ADD COLUMN telefone2 VARCHAR(20),
  ADD COLUMN whatsapp VARCHAR(20),
  ADD COLUMN email_alternativo VARCHAR(160),
  ADD COLUMN foto TEXT,
  ADD COLUMN observacoes TEXT;

-- Junction table for aluno-responsável links
CREATE TABLE IF NOT EXISTS aluno_responsaveis (
  id CHAR(36) NOT NULL,
  tenant_id CHAR(36) NOT NULL,
  aluno_id CHAR(36) NOT NULL,
  responsavel_id CHAR(36) NOT NULL,
  parentesco VARCHAR(30) NOT NULL DEFAULT 'outro',
  parentesco_descricao VARCHAR(100),
  responsavel_financeiro BOOLEAN NOT NULL DEFAULT FALSE,
  responsavel_academico BOOLEAN NOT NULL DEFAULT FALSE,
  autorizado_buscar BOOLEAN NOT NULL DEFAULT TRUE,
  principal BOOLEAN NOT NULL DEFAULT FALSE,
  observacoes TEXT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  created_by CHAR(36),
  updated_by CHAR(36),
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (id),
  CONSTRAINT fk_ar_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
  CONSTRAINT fk_ar_aluno FOREIGN KEY (aluno_id) REFERENCES alunos(id),
  CONSTRAINT fk_ar_responsavel FOREIGN KEY (responsavel_id) REFERENCES responsaveis(id)
);

CREATE INDEX idx_aluno_resp_tenant ON aluno_responsaveis(tenant_id);
CREATE INDEX idx_aluno_resp_aluno ON aluno_responsaveis(aluno_id);
CREATE INDEX idx_aluno_resp_responsavel ON aluno_responsaveis(responsavel_id);
