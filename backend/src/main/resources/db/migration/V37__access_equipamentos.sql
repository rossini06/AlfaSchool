-- =====================================================================
-- AlfaSchool Access — V37: equipamentos de leitura
--
-- Estende a tabela dispositivos ja existente em vez de criar outra.
--
-- Correcoes deliberadas em relacao aos sistemas anteriores do grupo:
--  1. senha do equipamento CIFRADA (AES-GCM), nunca em texto plano;
--  2. token de webhook POR DISPOSITIVO e guardado como hash — no AlfaGym
--     um unico segredo global permitia postar evento para qualquer tenant;
--  3. online expira por falta de heartbeat (nao fica "online" pra sempre).
-- =====================================================================

ALTER TABLE dispositivos
    ADD COLUMN portaria_id       CHAR(36)     NULL AFTER unit_id,
    ADD COLUMN funcao            VARCHAR(20)  NOT NULL DEFAULT 'ALUNO'      AFTER tipo,
    ADD COLUMN sentido           VARCHAR(20)  NOT NULL DEFAULT 'ENTRADA'    AFTER funcao,
    ADD COLUMN modo_sync         VARCHAR(20)  NOT NULL DEFAULT 'AGENTE'     AFTER sentido,
    ADD COLUMN login             VARCHAR(60)  NULL                          AFTER porta,
    ADD COLUMN senha_cifrada     VARBINARY(512) NULL                        AFTER login,
    ADD COLUMN grupo_acesso_id   INT          NOT NULL DEFAULT 1            AFTER senha_cifrada,
    ADD COLUMN webhook_token_hash VARCHAR(64) NULL                          AFTER api_token,
    ADD COLUMN ultimo_heartbeat  DATETIME(6)  NULL                          AFTER ultimo_ping,
    ADD COLUMN agent_version     VARCHAR(20)  NULL                          AFTER ultimo_heartbeat,
    ADD COLUMN ultima_leitura_log DATETIME(6) NULL                          AFTER agent_version,
    ADD COLUMN sincroniza_auto   BOOLEAN      NOT NULL DEFAULT TRUE         AFTER ativo;

ALTER TABLE dispositivos
    ADD CONSTRAINT fk_dispositivos_portaria FOREIGN KEY (portaria_id) REFERENCES acc_portarias(id);

CREATE INDEX idx_dispositivos_portaria ON dispositivos(tenant_id, portaria_id, ativo);
CREATE INDEX idx_dispositivos_serial   ON dispositivos(tenant_id, serial);

-- Zonas que o equipamento controla (um leitor pode dar acesso a mais de uma).
CREATE TABLE IF NOT EXISTS acc_dispositivo_zonas (
    id             CHAR(36)    NOT NULL,
    tenant_id      CHAR(36)    NOT NULL,
    dispositivo_id CHAR(36)    NOT NULL,
    zona_id        CHAR(36)    NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_acc_disp_zonas UNIQUE (dispositivo_id, zona_id),
    CONSTRAINT fk_acc_disp_zonas_tenant FOREIGN KEY (tenant_id)      REFERENCES tenants(tenant_id),
    CONSTRAINT fk_acc_disp_zonas_disp   FOREIGN KEY (dispositivo_id) REFERENCES dispositivos(id),
    CONSTRAINT fk_acc_disp_zonas_zona   FOREIGN KEY (zona_id)        REFERENCES acc_zonas(id)
);

-- Credencial do agente local (gateway na LAN da escola).
CREATE TABLE IF NOT EXISTS acc_agent_credenciais (
    id             CHAR(36)     NOT NULL,
    tenant_id      CHAR(36)     NOT NULL,
    unit_id        CHAR(36),
    username       VARCHAR(60)  NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    descricao      VARCHAR(255),
    ativo          BOOLEAN      NOT NULL DEFAULT TRUE,
    ultimo_login   DATETIME(6),
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    created_by     CHAR(36),
    updated_by     CHAR(36),
    deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_acc_agent_cred UNIQUE (tenant_id, username),
    CONSTRAINT fk_acc_agent_cred_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    CONSTRAINT fk_acc_agent_cred_unit   FOREIGN KEY (unit_id)   REFERENCES units(id)
);

-- Fila de comandos servidor -> agente (sincronizar pessoa, abrir porta,
-- remover biometria). O agente faz polling; o servidor nunca alcanca a LAN.
CREATE TABLE IF NOT EXISTS acc_agent_tasks (
    id             CHAR(36)    NOT NULL,
    tenant_id      CHAR(36)    NOT NULL,
    dispositivo_id CHAR(36),
    tipo           VARCHAR(40) NOT NULL,
    parametros     TEXT,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    resultado      TEXT,
    tentativas     INT         NOT NULL DEFAULT 0,
    expira_em      DATETIME(6),
    concluido_em   DATETIME(6),
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_acc_agent_tasks_tenant FOREIGN KEY (tenant_id)      REFERENCES tenants(tenant_id),
    CONSTRAINT fk_acc_agent_tasks_disp   FOREIGN KEY (dispositivo_id) REFERENCES dispositivos(id)
);
CREATE INDEX idx_acc_agent_tasks ON acc_agent_tasks(tenant_id, status, created_at);
