CREATE TABLE IF NOT EXISTS cobrancas (
    id           CHAR(36)      NOT NULL,
    tenant_id    CHAR(36)      NOT NULL,
    contrato_id  CHAR(36)      NOT NULL,
    aluno_id     CHAR(36)      NOT NULL,
    valor        DECIMAL(10,2) NOT NULL,
    descricao    VARCHAR(160),
    vencimento   DATE          NOT NULL,
    data_pagamento DATE,
    status       VARCHAR(20)   NOT NULL DEFAULT 'pendente',
    competencia  VARCHAR(7),
    created_at   DATETIME(6)   NOT NULL,
    updated_at   DATETIME(6)   NOT NULL,
    created_by   CHAR(36),
    updated_by   CHAR(36),
    deleted      BOOLEAN       NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

CREATE INDEX idx_cobrancas_tenant     ON cobrancas(tenant_id);
CREATE INDEX idx_cobrancas_contrato   ON cobrancas(contrato_id);
CREATE INDEX idx_cobrancas_aluno      ON cobrancas(aluno_id);
CREATE INDEX idx_cobrancas_status     ON cobrancas(status);
CREATE INDEX idx_cobrancas_vencimento ON cobrancas(vencimento);
