CREATE TABLE IF NOT EXISTS saas_plans (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    nome VARCHAR(80) NOT NULL,
    slug VARCHAR(40) NOT NULL,
    descricao TEXT,
    preco_mensal DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    preco_anual DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    max_escolas INT NOT NULL DEFAULT -1,
    max_usuarios INT NOT NULL DEFAULT -1,
    max_dispositivos INT NOT NULL DEFAULT -1,
    recursos JSON,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by CHAR(36),
    updated_by CHAR(36),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_saas_plans_slug UNIQUE (slug, tenant_id)
);

ALTER TABLE tenants ADD COLUMN status_saas VARCHAR(20) NOT NULL DEFAULT 'TRIAL';
ALTER TABLE tenants ADD COLUMN trial_end DATE;
ALTER TABLE tenants ADD COLUMN plano_id CHAR(36);
ALTER TABLE tenants ADD COLUMN obs TEXT;

CREATE INDEX idx_saas_plans_tenant ON saas_plans(tenant_id);
CREATE INDEX idx_saas_plans_slug ON saas_plans(slug);
