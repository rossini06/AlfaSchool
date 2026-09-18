-- =====================================================================
-- AlfaSchool Access — V34: jornada contratada (horas que o pai contratou)
--
-- Uma MESMA turma tem alunos com jornadas diferentes: o Pedro sai ao meio
-- dia, a Ana fica ate as 17h. Por isso a jornada e' vinculada ao ALUNO,
-- com vigencia, e nao a' turma.
--
-- Esta e' a base do calculo de excedente. Nao confundir com frequencia
-- pedagogica: um aluno pode ficar 10h na escola e ter 4h de aula.
-- =====================================================================

CREATE TABLE IF NOT EXISTS acc_jornadas (
    id                     CHAR(36)     NOT NULL,
    tenant_id              CHAR(36)     NOT NULL,
    nome                   VARCHAR(120) NOT NULL,
    descricao              VARCHAR(255),
    -- franquia contratual antes de considerar excedente/atraso
    tolerancia_entrada_min INT          NOT NULL DEFAULT 0,
    tolerancia_saida_min   INT          NOT NULL DEFAULT 0,
    -- DURACAO  = excedente pelo total de horas no dia
    -- HORARIO  = excedente por sair depois da saida prevista
    -- AMBOS    = o maior dos dois
    regra_excedente        VARCHAR(20)  NOT NULL DEFAULT 'HORARIO',
    ativo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at             DATETIME(6)  NOT NULL,
    updated_at             DATETIME(6)  NOT NULL,
    created_by             CHAR(36),
    updated_by             CHAR(36),
    deleted                BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_acc_jornadas_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
);
CREATE INDEX idx_acc_jornadas_tenant ON acc_jornadas(tenant_id, ativo);

-- dia_semana: 1=segunda ... 7=domingo
CREATE TABLE IF NOT EXISTS acc_jornada_dias (
    id                CHAR(36)    NOT NULL,
    tenant_id         CHAR(36)    NOT NULL,
    jornada_id        CHAR(36)    NOT NULL,
    dia_semana        INT         NOT NULL,
    frequenta         BOOLEAN     NOT NULL DEFAULT TRUE,
    entrada_prevista  TIME,
    saida_prevista    TIME,
    carga_minutos     INT         NOT NULL DEFAULT 0,
    created_at        DATETIME(6) NOT NULL,
    updated_at        DATETIME(6) NOT NULL,
    created_by        CHAR(36),
    updated_by        CHAR(36),
    deleted           BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_acc_jornada_dias UNIQUE (jornada_id, dia_semana),
    CONSTRAINT fk_acc_jornada_dias_tenant  FOREIGN KEY (tenant_id)  REFERENCES tenants(tenant_id),
    CONSTRAINT fk_acc_jornada_dias_jornada FOREIGN KEY (jornada_id) REFERENCES acc_jornadas(id)
);

-- Vigencia permite trocar o plano no meio do ano sem reescrever o passado:
-- a apuracao de marco continua usando a jornada que valia em marco.
CREATE TABLE IF NOT EXISTS acc_aluno_jornadas (
    id               CHAR(36)    NOT NULL,
    tenant_id        CHAR(36)    NOT NULL,
    aluno_id         CHAR(36)    NOT NULL,
    jornada_id       CHAR(36)    NOT NULL,
    vigencia_inicio  DATE        NOT NULL,
    vigencia_fim     DATE,
    observacao       VARCHAR(255),
    created_at       DATETIME(6) NOT NULL,
    updated_at       DATETIME(6) NOT NULL,
    created_by       CHAR(36),
    updated_by       CHAR(36),
    deleted          BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_acc_aluno_jornadas_tenant  FOREIGN KEY (tenant_id)  REFERENCES tenants(tenant_id),
    CONSTRAINT fk_acc_aluno_jornadas_aluno   FOREIGN KEY (aluno_id)   REFERENCES alunos(id),
    CONSTRAINT fk_acc_aluno_jornadas_jornada FOREIGN KEY (jornada_id) REFERENCES acc_jornadas(id)
);
CREATE INDEX idx_acc_aluno_jornadas ON acc_aluno_jornadas(tenant_id, aluno_id, vigencia_inicio);

-- Excecao pontual: "hoje a Ana sai as 13h porque tem consulta".
CREATE TABLE IF NOT EXISTS acc_jornada_excecoes (
    id                CHAR(36)    NOT NULL,
    tenant_id         CHAR(36)    NOT NULL,
    aluno_id          CHAR(36)    NOT NULL,
    data              DATE        NOT NULL,
    frequenta         BOOLEAN     NOT NULL DEFAULT TRUE,
    entrada_prevista  TIME,
    saida_prevista    TIME,
    carga_minutos     INT,
    motivo            VARCHAR(255),
    created_at        DATETIME(6) NOT NULL,
    updated_at        DATETIME(6) NOT NULL,
    created_by        CHAR(36),
    updated_by        CHAR(36),
    deleted           BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_acc_jornada_excecoes UNIQUE (aluno_id, data),
    CONSTRAINT fk_acc_jornada_exc_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    CONSTRAINT fk_acc_jornada_exc_aluno  FOREIGN KEY (aluno_id)  REFERENCES alunos(id)
);
