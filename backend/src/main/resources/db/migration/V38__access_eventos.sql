-- =====================================================================
-- AlfaSchool Access — V38: eventos brutos de leitura
--
-- Esta tabela e' o LIVRO-RAZAO do modulo: tudo o que o leitor viu,
-- inclusive acesso negado e pessoa desconhecida. Nada e' apagado aqui;
-- interpretacao (presenca, retirada) vive em outras tabelas.
--
-- Idempotencia: o par (dispositivo_id, device_log_id) identifica o
-- evento no equipamento. O indice NAO e' unique de proposito — leitores
-- reiniciam o contador ao ter o historico limpo, e um unique
-- transformaria isso num buraco negro permanente de eventos perdidos.
-- A deduplicacao usa o par + janela de tempo curta.
-- =====================================================================

-- titular_tipo: ALUNO | RESPONSAVEL | COLABORADOR | AUTORIZADA | DESCONHECIDO
-- tipo:         FACE | CARTAO | BIOMETRIA | MANUAL
-- resultado:    PERMITIDO | NEGADO | DESCONHECIDO
-- sentido:      ENTRADA | SAIDA | INDEFINIDO
CREATE TABLE IF NOT EXISTS acc_eventos (
    id              CHAR(36)     NOT NULL,
    tenant_id       CHAR(36)     NOT NULL,
    unit_id         CHAR(36),
    dispositivo_id  CHAR(36),
    portaria_id     CHAR(36),
    device_log_id   BIGINT,
    device_user_id  BIGINT,
    titular_tipo    VARCHAR(20)  NOT NULL DEFAULT 'DESCONHECIDO',
    titular_id      CHAR(36),
    tipo            VARCHAR(20)  NOT NULL DEFAULT 'FACE',
    resultado       VARCHAR(20)  NOT NULL DEFAULT 'PERMITIDO',
    sentido         VARCHAR(20)  NOT NULL DEFAULT 'INDEFINIDO',
    motivo          VARCHAR(255),
    data_hora       DATETIME(6)  NOT NULL,
    recebido_em     DATETIME(6)  NOT NULL,
    origem          VARCHAR(20)  NOT NULL DEFAULT 'AGENTE',
    raw_json        TEXT,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_acc_eventos_tenant FOREIGN KEY (tenant_id)      REFERENCES tenants(tenant_id),
    CONSTRAINT fk_acc_eventos_disp   FOREIGN KEY (dispositivo_id) REFERENCES dispositivos(id),
    CONSTRAINT fk_acc_eventos_port   FOREIGN KEY (portaria_id)    REFERENCES acc_portarias(id)
);
CREATE INDEX idx_acc_eventos_dedup   ON acc_eventos(dispositivo_id, device_log_id, data_hora);
CREATE INDEX idx_acc_eventos_periodo ON acc_eventos(tenant_id, data_hora);
CREATE INDEX idx_acc_eventos_titular ON acc_eventos(tenant_id, titular_tipo, titular_id, data_hora);
