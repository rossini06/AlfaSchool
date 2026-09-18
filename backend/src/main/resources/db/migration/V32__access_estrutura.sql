-- =====================================================================
-- AlfaSchool Access — V32: estrutura institucional do acesso
-- Separa deliberadamente tres conceitos que a operacao costuma misturar:
--   portaria  = ponto fisico de entrada/saida da unidade
--   zona      = area interna cujo acesso e' controlado
--   sala      = espaco fisico onde a turma fica (uma sala abriga N turmas)
-- =====================================================================

CREATE TABLE IF NOT EXISTS acc_portarias (
    id          CHAR(36)     NOT NULL,
    tenant_id   CHAR(36)     NOT NULL,
    unit_id     CHAR(36)     NOT NULL,
    nome        VARCHAR(120) NOT NULL,
    tipo        VARCHAR(20)  NOT NULL DEFAULT 'PRINCIPAL',
    descricao   VARCHAR(255),
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    created_by  CHAR(36),
    updated_by  CHAR(36),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_acc_portarias_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    CONSTRAINT fk_acc_portarias_unit   FOREIGN KEY (unit_id)   REFERENCES units(id)
);
CREATE INDEX idx_acc_portarias_unit ON acc_portarias(tenant_id, unit_id, ativo);

CREATE TABLE IF NOT EXISTS acc_zonas (
    id          CHAR(36)     NOT NULL,
    tenant_id   CHAR(36)     NOT NULL,
    unit_id     CHAR(36)     NOT NULL,
    nome        VARCHAR(120) NOT NULL,
    descricao   VARCHAR(255),
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    created_by  CHAR(36),
    updated_by  CHAR(36),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_acc_zonas_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    CONSTRAINT fk_acc_zonas_unit   FOREIGN KEY (unit_id)   REFERENCES units(id)
);
CREATE INDEX idx_acc_zonas_unit ON acc_zonas(tenant_id, unit_id, ativo);

CREATE TABLE IF NOT EXISTS acc_salas (
    id          CHAR(36)     NOT NULL,
    tenant_id   CHAR(36)     NOT NULL,
    unit_id     CHAR(36)     NOT NULL,
    zona_id     CHAR(36),
    nome        VARCHAR(120) NOT NULL,
    codigo      VARCHAR(30),
    bloco       VARCHAR(60),
    andar       VARCHAR(30),
    capacidade  INT,
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    created_by  CHAR(36),
    updated_by  CHAR(36),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_acc_salas_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    CONSTRAINT fk_acc_salas_unit   FOREIGN KEY (unit_id)   REFERENCES units(id),
    CONSTRAINT fk_acc_salas_zona   FOREIGN KEY (zona_id)   REFERENCES acc_zonas(id)
);
CREATE INDEX idx_acc_salas_unit ON acc_salas(tenant_id, unit_id, ativo);

-- Uma turma pode mudar de sala ao longo do dia ou do ano letivo; por isso
-- o vinculo tem vigencia e, opcionalmente, faixa de horario e dias da semana.
-- dias_semana e' uma mascara CSV "1,2,3,4,5" (1=segunda ... 7=domingo).
CREATE TABLE IF NOT EXISTS acc_turma_salas (
    id               CHAR(36)    NOT NULL,
    tenant_id        CHAR(36)    NOT NULL,
    turma_id         CHAR(36)    NOT NULL,
    sala_id          CHAR(36)    NOT NULL,
    vigencia_inicio  DATE        NOT NULL,
    vigencia_fim     DATE,
    hora_inicio      TIME,
    hora_fim         TIME,
    dias_semana      VARCHAR(20),
    created_at       DATETIME(6) NOT NULL,
    updated_at       DATETIME(6) NOT NULL,
    created_by       CHAR(36),
    updated_by       CHAR(36),
    deleted          BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_acc_turma_salas_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    CONSTRAINT fk_acc_turma_salas_turma  FOREIGN KEY (turma_id)  REFERENCES turmas(id),
    CONSTRAINT fk_acc_turma_salas_sala   FOREIGN KEY (sala_id)   REFERENCES acc_salas(id)
);
CREATE INDEX idx_acc_turma_salas_turma ON acc_turma_salas(tenant_id, turma_id, vigencia_inicio);
CREATE INDEX idx_acc_turma_salas_sala  ON acc_turma_salas(tenant_id, sala_id, vigencia_inicio);
