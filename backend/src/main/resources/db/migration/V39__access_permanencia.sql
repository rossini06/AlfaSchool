-- =====================================================================
-- AlfaSchool Access — V39: permanencia do aluno
--
-- REGRA QUE NAO PODE SER VIOLADA:
-- a permanencia so' encerra quando ha SAIDA EFETIVA confirmada. O pai
-- chegar as 17h nao encerra nada; se a crianca so' foi entregue as 17h20,
-- a permanencia vai ate 17h20.
--
-- Um dia pode ter varios pares entrada/saida (aluno sai para consulta e
-- volta). Por isso acc_presencas e' o dia e acc_presenca_pares sao os
-- intervalos. O total do dia e' a soma dos pares fechados.
-- =====================================================================

-- status: ABERTA | FECHADA | INCONSISTENTE | AJUSTADA
CREATE TABLE IF NOT EXISTS acc_presencas (
    id                   CHAR(36)    NOT NULL,
    tenant_id            CHAR(36)    NOT NULL,
    unit_id              CHAR(36),
    aluno_id             CHAR(36)    NOT NULL,
    data                 DATE        NOT NULL,
    primeira_entrada_em  DATETIME(6),
    ultima_saida_em      DATETIME(6),
    minutos_permanencia  INT         NOT NULL DEFAULT 0,
    minutos_previstos    INT         NOT NULL DEFAULT 0,
    minutos_excedente    INT         NOT NULL DEFAULT 0,
    minutos_antecipacao  INT         NOT NULL DEFAULT 0,
    jornada_id           CHAR(36),
    dia_letivo           BOOLEAN     NOT NULL DEFAULT TRUE,
    status               VARCHAR(20) NOT NULL DEFAULT 'ABERTA',
    -- snapshot congelado: apos o fechamento do mes o calculo nao muda mais,
    -- mesmo que alguem edite a jornada depois. Fatura emitida nao se altera.
    congelada            BOOLEAN     NOT NULL DEFAULT FALSE,
    congelada_em         DATETIME(6),
    observacao           VARCHAR(255),
    created_at           DATETIME(6) NOT NULL,
    updated_at           DATETIME(6) NOT NULL,
    created_by           CHAR(36),
    updated_by           CHAR(36),
    deleted              BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_acc_presencas UNIQUE (aluno_id, data),
    CONSTRAINT fk_acc_presencas_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    CONSTRAINT fk_acc_presencas_aluno  FOREIGN KEY (aluno_id)  REFERENCES alunos(id)
);
CREATE INDEX idx_acc_presencas_data   ON acc_presencas(tenant_id, data, status);
CREATE INDEX idx_acc_presencas_aberta ON acc_presencas(tenant_id, status, data);

-- origem: EVENTO | MANUAL — ajuste manual sempre identifica o operador.
CREATE TABLE IF NOT EXISTS acc_presenca_pares (
    id                CHAR(36)    NOT NULL,
    tenant_id         CHAR(36)    NOT NULL,
    presenca_id       CHAR(36)    NOT NULL,
    entrada_em        DATETIME(6) NOT NULL,
    saida_em          DATETIME(6),
    minutos           INT,
    entrada_evento_id CHAR(36),
    saida_evento_id   CHAR(36),
    origem            VARCHAR(20) NOT NULL DEFAULT 'EVENTO',
    ajustado_por      CHAR(36),
    motivo_ajuste     VARCHAR(255),
    created_at        DATETIME(6) NOT NULL,
    updated_at        DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_acc_pres_pares_tenant   FOREIGN KEY (tenant_id)   REFERENCES tenants(tenant_id),
    CONSTRAINT fk_acc_pres_pares_presenca FOREIGN KEY (presenca_id) REFERENCES acc_presencas(id)
);
CREATE INDEX idx_acc_presenca_pares ON acc_presenca_pares(tenant_id, presenca_id, entrada_em);

-- Fechamento mensal por unidade: congela o periodo para faturamento.
CREATE TABLE IF NOT EXISTS acc_fechamentos (
    id            CHAR(36)    NOT NULL,
    tenant_id     CHAR(36)    NOT NULL,
    unit_id       CHAR(36),
    competencia   VARCHAR(7)  NOT NULL,
    data_inicio   DATE        NOT NULL,
    data_fim      DATE        NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'ABERTO',
    fechado_por   CHAR(36),
    fechado_em    DATETIME(6),
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_acc_fechamentos UNIQUE (tenant_id, unit_id, competencia),
    CONSTRAINT fk_acc_fechamentos_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
);
