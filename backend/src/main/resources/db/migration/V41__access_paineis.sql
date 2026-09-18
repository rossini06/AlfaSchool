-- =====================================================================
-- AlfaSchool Access — V41: ambientes digitais (paineis)
--
-- Ambiente digital NAO e' o espaco fisico. E' uma TELA que recebe eventos
-- de um recorte: uma turma, uma sala, uma portaria ou a unidade inteira.
--
-- SEGURANCA: a URL permanente identifica o painel, mas NAO autentica.
-- Cada TV precisa de um dispositivo autorizado com token proprio e
-- revogavel. Sem isso, qualquer navegador com o link veria fotos de
-- criancas. O painel de uma sala mostra APENAS os alunos daquela sala.
-- =====================================================================

-- tipo: COORDENACAO | SALA | PORTARIA | UNIDADE
CREATE TABLE IF NOT EXISTS acc_paineis (
    id            CHAR(36)     NOT NULL,
    tenant_id     CHAR(36)     NOT NULL,
    unit_id       CHAR(36)     NOT NULL,
    nome          VARCHAR(120) NOT NULL,
    slug          VARCHAR(60)  NOT NULL,
    tipo          VARCHAR(20)  NOT NULL DEFAULT 'SALA',
    -- exibe foto do aluno/responsavel nesta tela?
    exibe_foto    BOOLEAN      NOT NULL DEFAULT TRUE,
    -- segundos que o cartao permanece na tela apos a entrega
    retencao_seg  INT          NOT NULL DEFAULT 20,
    ativo         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    created_by    CHAR(36),
    updated_by    CHAR(36),
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_acc_paineis_slug UNIQUE (tenant_id, slug),
    CONSTRAINT fk_acc_paineis_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    CONSTRAINT fk_acc_paineis_unit   FOREIGN KEY (unit_id)   REFERENCES units(id)
);
CREATE INDEX idx_acc_paineis_unit ON acc_paineis(tenant_id, unit_id, ativo);

-- Recorte do painel. escopo: TURMA | SALA | PORTARIA | UNIDADE
CREATE TABLE IF NOT EXISTS acc_painel_fontes (
    id            CHAR(36)    NOT NULL,
    tenant_id     CHAR(36)    NOT NULL,
    painel_id     CHAR(36)    NOT NULL,
    escopo        VARCHAR(20) NOT NULL,
    referencia_id CHAR(36),
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_acc_painel_fontes UNIQUE (painel_id, escopo, referencia_id),
    CONSTRAINT fk_acc_painel_fontes_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    CONSTRAINT fk_acc_painel_fontes_painel FOREIGN KEY (painel_id) REFERENCES acc_paineis(id)
);

-- A TV. Token guardado como hash; revogacao imediata.
CREATE TABLE IF NOT EXISTS acc_painel_dispositivos (
    id            CHAR(36)     NOT NULL,
    tenant_id     CHAR(36)     NOT NULL,
    painel_id     CHAR(36)     NOT NULL,
    nome          VARCHAR(120) NOT NULL,
    token_hash    VARCHAR(64)  NOT NULL,
    token_prefixo VARCHAR(12),
    ultimo_acesso DATETIME(6),
    ultimo_ip     VARCHAR(45),
    user_agent    VARCHAR(255),
    revogado      BOOLEAN      NOT NULL DEFAULT FALSE,
    revogado_em   DATETIME(6),
    revogado_por  CHAR(36),
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    created_by    CHAR(36),
    updated_by    CHAR(36),
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_acc_painel_disp_token UNIQUE (token_hash),
    CONSTRAINT fk_acc_painel_disp_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    CONSTRAINT fk_acc_painel_disp_painel FOREIGN KEY (painel_id) REFERENCES acc_paineis(id)
);
CREATE INDEX idx_acc_painel_disp ON acc_painel_dispositivos(tenant_id, painel_id, revogado);
