-- =====================================================================
-- AlfaSchool Access — V42: motor central de notificacoes
--
-- Um motor unico, com fila em tabela, em vez de disparar mensagem
-- espalhada por cada funcionalidade. Canal e' abstrato: EMAIL e WHATSAPP
-- usam a mesma fila, o mesmo retry e o mesmo historico de entrega.
--
-- Nunca enviar foto, dado biometrico ou detalhe excessivo de crianca em
-- mensagem automatica.
-- =====================================================================

-- Configuracao de canal por escola. Segredos do provedor ficam cifrados.
CREATE TABLE IF NOT EXISTS acc_notificacao_configs (
    id            CHAR(36)     NOT NULL,
    tenant_id     CHAR(36)     NOT NULL,
    canal         VARCHAR(20)  NOT NULL,
    provider      VARCHAR(40)  NOT NULL,
    remetente     VARCHAR(160),
    config_json   TEXT,
    segredo_cifrado VARBINARY(2048),
    ativo         BOOLEAN      NOT NULL DEFAULT FALSE,
    -- teto diario protege contra estouro de cota do provedor no pico
    -- de entrada/saida, quando centenas de avisos saem em minutos
    limite_diario INT          NOT NULL DEFAULT 1000,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    created_by    CHAR(36),
    updated_by    CHAR(36),
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_acc_notif_configs UNIQUE (tenant_id, canal),
    CONSTRAINT fk_acc_notif_cfg_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
);

-- evento: ENTRADA_CONFIRMADA | SAIDA_CONFIRMADA | JORNADA_PROXIMA_FIM |
--         HORARIO_EXCEDIDO | TENTATIVA_NAO_AUTORIZADA | EQUIPAMENTO_OFFLINE |
--         RETIRADA_SOLICITADA | AUTORIZACAO_APROVADA
CREATE TABLE IF NOT EXISTS acc_notificacao_templates (
    id            CHAR(36)     NOT NULL,
    tenant_id     CHAR(36)     NOT NULL,
    evento        VARCHAR(40)  NOT NULL,
    canal         VARCHAR(20)  NOT NULL,
    assunto       VARCHAR(255),
    corpo         TEXT         NOT NULL,
    -- nome do template aprovado na Meta, quando canal = WHATSAPP
    template_externo VARCHAR(120),
    ativo         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    created_by    CHAR(36),
    updated_by    CHAR(36),
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_acc_notif_templates UNIQUE (tenant_id, evento, canal),
    CONSTRAINT fk_acc_notif_tpl_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
);

-- Consentimento por pessoa/canal/evento. Opt-in explicito e revogavel.
CREATE TABLE IF NOT EXISTS acc_notificacao_preferencias (
    id            CHAR(36)     NOT NULL,
    tenant_id     CHAR(36)     NOT NULL,
    titular_tipo  VARCHAR(20)  NOT NULL,
    titular_id    CHAR(36)     NOT NULL,
    canal         VARCHAR(20)  NOT NULL,
    evento        VARCHAR(40),
    destino       VARCHAR(160) NOT NULL,
    habilitado    BOOLEAN      NOT NULL DEFAULT TRUE,
    opt_in_em     DATETIME(6),
    opt_out_em    DATETIME(6),
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    created_by    CHAR(36),
    updated_by    CHAR(36),
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_acc_notif_pref_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
);
CREATE INDEX idx_acc_notif_pref ON acc_notificacao_preferencias(tenant_id, titular_tipo, titular_id, canal);

-- Fila de envio. status: PENDENTE | ENVIANDO | ENVIADO | FALHOU | EXPIRADO | CANCELADO
-- chave_idempotencia evita o mesmo aviso sair duas vezes quando o evento
-- e' reprocessado (ex.: reenvio da fila offline do agente).
CREATE TABLE IF NOT EXISTS acc_notificacao_envios (
    id                   CHAR(36)     NOT NULL,
    tenant_id            CHAR(36)     NOT NULL,
    canal                VARCHAR(20)  NOT NULL,
    evento               VARCHAR(40)  NOT NULL,
    titular_tipo         VARCHAR(20),
    titular_id           CHAR(36),
    aluno_id             CHAR(36),
    destino              VARCHAR(160) NOT NULL,
    assunto              VARCHAR(255),
    corpo                TEXT,
    payload_json         TEXT,
    status               VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE',
    tentativas           INT          NOT NULL DEFAULT 0,
    erro                 VARCHAR(500),
    provider_message_id  VARCHAR(120),
    chave_idempotencia   VARCHAR(160),
    agendado_para        DATETIME(6),
    enviado_em           DATETIME(6),
    entregue_em          DATETIME(6),
    created_at           DATETIME(6)  NOT NULL,
    updated_at           DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_acc_notif_envios_idem UNIQUE (tenant_id, chave_idempotencia),
    CONSTRAINT fk_acc_notif_env_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
);
CREATE INDEX idx_acc_notif_envios_fila ON acc_notificacao_envios(status, agendado_para);
CREATE INDEX idx_acc_notif_envios_hist ON acc_notificacao_envios(tenant_id, created_at);
