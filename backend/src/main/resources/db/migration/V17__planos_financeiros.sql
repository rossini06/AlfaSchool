CREATE TABLE IF NOT EXISTS planos_financeiros (
    id             CHAR(36)      NOT NULL,
    tenant_id      CHAR(36)      NOT NULL,
    nome           VARCHAR(120)  NOT NULL,
    valor          DECIMAL(10,2) NOT NULL,
    periodicidade  VARCHAR(20)   NOT NULL DEFAULT 'mensal',
    descricao      TEXT,
    ativo          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     DATETIME(6)   NOT NULL,
    updated_at     DATETIME(6)   NOT NULL,
    created_by     CHAR(36),
    updated_by     CHAR(36),
    deleted        BOOLEAN       NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

CREATE INDEX idx_planos_fin_tenant ON planos_financeiros(tenant_id);
