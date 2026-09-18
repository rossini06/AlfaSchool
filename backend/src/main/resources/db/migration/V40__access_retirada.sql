-- =====================================================================
-- AlfaSchool Access — V40: fila de retirada
--
-- TRES EVENTOS DISTINTOS, nunca colapsados num so':
--   1. solicitado_em  — o responsavel foi reconhecido na portaria
--   2. entregue_em    — um colaborador confirmou a entrega da crianca
--   3. saida_em       — a saida efetiva foi registrada (fecha a permanencia)
--
-- Reconhecer o rosto do pai NAO entrega o aluno e NAO encerra a
-- permanencia. Entre a chegada e a entrega existe tempo real de espera,
-- e e' esse tempo que o relatorio de espera mede.
--
-- Nenhuma transicao de status pode ser feita sem usuario identificado.
-- =====================================================================

-- status: SOLICITADA | PREPARANDO | PRONTO | ENTREGUE | CANCELADA | NEGADA
CREATE TABLE IF NOT EXISTS acc_retiradas (
    id                     CHAR(36)    NOT NULL,
    tenant_id              CHAR(36)    NOT NULL,
    unit_id                CHAR(36),
    aluno_id               CHAR(36)    NOT NULL,
    pessoa_autorizada_id   CHAR(36),
    autorizacao_id         CHAR(36),
    portaria_id            CHAR(36),
    dispositivo_id         CHAR(36),
    turma_id               CHAR(36),
    sala_id                CHAR(36),
    status                 VARCHAR(20) NOT NULL DEFAULT 'SOLICITADA',
    ordem_chegada          INT,
    -- 1. chegada do responsavel
    solicitado_em          DATETIME(6),
    solicitacao_evento_id  CHAR(36),
    -- 2. preparo na sala
    preparando_em          DATETIME(6),
    preparado_por_user_id  CHAR(36),
    pronto_em              DATETIME(6),
    -- 3. entrega confirmada por colaborador
    entregue_em            DATETIME(6),
    entregue_por_user_id   CHAR(36),
    -- 4. saida efetiva (fecha a permanencia)
    saida_em               DATETIME(6),
    saida_evento_id        CHAR(36),
    cancelado_em           DATETIME(6),
    cancelado_por_user_id  CHAR(36),
    motivo                 VARCHAR(255),
    retirada_manual        BOOLEAN     NOT NULL DEFAULT FALSE,
    observacao             TEXT,
    created_at             DATETIME(6) NOT NULL,
    updated_at             DATETIME(6) NOT NULL,
    created_by             CHAR(36),
    updated_by             CHAR(36),
    deleted                BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_acc_retiradas_tenant  FOREIGN KEY (tenant_id)            REFERENCES tenants(tenant_id),
    CONSTRAINT fk_acc_retiradas_aluno   FOREIGN KEY (aluno_id)             REFERENCES alunos(id),
    CONSTRAINT fk_acc_retiradas_pessoa  FOREIGN KEY (pessoa_autorizada_id) REFERENCES acc_pessoas_autorizadas(id),
    CONSTRAINT fk_acc_retiradas_aut     FOREIGN KEY (autorizacao_id)       REFERENCES acc_autorizacoes_retirada(id),
    CONSTRAINT fk_acc_retiradas_port    FOREIGN KEY (portaria_id)          REFERENCES acc_portarias(id)
);
CREATE INDEX idx_acc_retiradas_fila  ON acc_retiradas(tenant_id, status, solicitado_em);
CREATE INDEX idx_acc_retiradas_aluno ON acc_retiradas(tenant_id, aluno_id, created_at);
CREATE INDEX idx_acc_retiradas_turma ON acc_retiradas(tenant_id, turma_id, status);

-- Trilha de transicoes: quem mudou o status, quando, de onde.
CREATE TABLE IF NOT EXISTS acc_retirada_historico (
    id              CHAR(36)    NOT NULL,
    tenant_id       CHAR(36)    NOT NULL,
    retirada_id     CHAR(36)    NOT NULL,
    status_anterior VARCHAR(20),
    status_novo     VARCHAR(20) NOT NULL,
    user_id         CHAR(36),
    origem          VARCHAR(20) NOT NULL DEFAULT 'PAINEL',
    motivo          VARCHAR(255),
    ip              VARCHAR(45),
    created_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_acc_ret_hist_tenant   FOREIGN KEY (tenant_id)   REFERENCES tenants(tenant_id),
    CONSTRAINT fk_acc_ret_hist_retirada FOREIGN KEY (retirada_id) REFERENCES acc_retiradas(id)
);
CREATE INDEX idx_acc_ret_hist ON acc_retirada_historico(tenant_id, retirada_id, created_at);

-- Ocorrencias que exigem intervencao humana.
-- tipo: TENTATIVA_NAO_AUTORIZADA | PESSOA_DESCONHECIDA | RESTRICAO_JUDICIAL |
--       RETIRADA_MANUAL | HORARIO_EXCEDIDO | EQUIPAMENTO_OFFLINE |
--       SAIDA_SEM_REGISTRO | ENTRADA_DUPLICADA
CREATE TABLE IF NOT EXISTS acc_ocorrencias (
    id                CHAR(36)    NOT NULL,
    tenant_id         CHAR(36)    NOT NULL,
    unit_id           CHAR(36),
    tipo              VARCHAR(40) NOT NULL,
    gravidade         VARCHAR(20) NOT NULL DEFAULT 'MEDIA',
    aluno_id          CHAR(36),
    pessoa_autorizada_id CHAR(36),
    dispositivo_id    CHAR(36),
    evento_id         CHAR(36),
    retirada_id       CHAR(36),
    descricao         TEXT        NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'ABERTA',
    tratado_por_user_id CHAR(36),
    tratado_em        DATETIME(6),
    tratativa         TEXT,
    created_at        DATETIME(6) NOT NULL,
    updated_at        DATETIME(6) NOT NULL,
    created_by        CHAR(36),
    updated_by        CHAR(36),
    deleted           BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_acc_ocorrencias_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
);
CREATE INDEX idx_acc_ocorrencias ON acc_ocorrencias(tenant_id, status, gravidade, created_at);
