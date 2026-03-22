CREATE TABLE IF NOT EXISTS dispositivos (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    unit_id CHAR(36),
    nome VARCHAR(120) NOT NULL,
    tipo VARCHAR(40) NOT NULL DEFAULT 'catraca',
    fabricante VARCHAR(80),
    modelo VARCHAR(80),
    ip VARCHAR(45),
    porta INT DEFAULT 80,
    serial VARCHAR(80),
    api_token VARCHAR(255),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    online BOOLEAN NOT NULL DEFAULT FALSE,
    ultimo_ping DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by CHAR(36),
    updated_by CHAR(36),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_dispositivos_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    CONSTRAINT fk_dispositivos_unit FOREIGN KEY (unit_id) REFERENCES units(id)
);
CREATE INDEX idx_dispositivos_tenant ON dispositivos(tenant_id);
CREATE INDEX idx_dispositivos_unit ON dispositivos(unit_id);
