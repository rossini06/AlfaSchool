-- =====================================================================
-- AlfaSchool Access — V33: calendario letivo
-- Dia letivo e feriado NAO sao a mesma coisa que "dia em que o aluno
-- frequenta": a jornada contratada (V34) decide isso. O calendario diz
-- se a escola abre; a jornada diz se aquele aluno deveria estar la.
-- =====================================================================

CREATE TABLE IF NOT EXISTS acc_calendarios (
    id          CHAR(36)     NOT NULL,
    tenant_id   CHAR(36)     NOT NULL,
    unit_id     CHAR(36),
    nome        VARCHAR(120) NOT NULL,
    ano_letivo  INT          NOT NULL,
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    created_by  CHAR(36),
    updated_by  CHAR(36),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_acc_calendarios_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    CONSTRAINT fk_acc_calendarios_unit   FOREIGN KEY (unit_id)   REFERENCES units(id)
);
CREATE INDEX idx_acc_calendarios_ano ON acc_calendarios(tenant_id, ano_letivo, ativo);

-- tipo: LETIVO | FERIADO | RECESSO | FACULTATIVO | SABADO_LETIVO | EVENTO
CREATE TABLE IF NOT EXISTS acc_calendario_dias (
    id             CHAR(36)     NOT NULL,
    tenant_id      CHAR(36)     NOT NULL,
    calendario_id  CHAR(36)     NOT NULL,
    data           DATE         NOT NULL,
    tipo           VARCHAR(20)  NOT NULL,
    descricao      VARCHAR(255),
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    created_by     CHAR(36),
    updated_by     CHAR(36),
    deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_acc_calendario_dias UNIQUE (calendario_id, data),
    CONSTRAINT fk_acc_cal_dias_tenant     FOREIGN KEY (tenant_id)     REFERENCES tenants(tenant_id),
    CONSTRAINT fk_acc_cal_dias_calendario FOREIGN KEY (calendario_id) REFERENCES acc_calendarios(id)
);
CREATE INDEX idx_acc_calendario_dias_data ON acc_calendario_dias(tenant_id, data, tipo);
