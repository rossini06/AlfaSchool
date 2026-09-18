-- =====================================================================
-- AlfaSchool Access — V36: biometria facial com base legal (LGPD)
--
-- O matching facial acontece 100% NO EQUIPAMENTO. O sistema so' guarda
-- a foto de referencia e sincroniza com os leitores.
--
-- Dado biometrico de CRIANCA e' dado sensivel com regime reforcado
-- (LGPD Art. 11 + Art. 14: melhor interesse, consentimento de ao menos
-- um dos pais). Por isso base legal e consentimento sao obrigatorios
-- e versionados, e a exportacao para o leitor e' bloqueada sem eles.
-- =====================================================================

-- titular_tipo: ALUNO | RESPONSAVEL | COLABORADOR | AUTORIZADA
CREATE TABLE IF NOT EXISTS acc_faces (
    id                    CHAR(36)     NOT NULL,
    tenant_id             CHAR(36)     NOT NULL,
    titular_tipo          VARCHAR(20)  NOT NULL,
    titular_id            CHAR(36)     NOT NULL,
    foto_key              VARCHAR(255) NOT NULL,
    -- id numerico usado como user_id dentro do equipamento Control iD
    device_user_id        BIGINT       NOT NULL,
    base_legal            VARCHAR(40),
    consentimento_obtido  BOOLEAN      NOT NULL DEFAULT FALSE,
    consentimento_em      DATETIME(6),
    consentimento_versao  VARCHAR(20),
    consentimento_origem  VARCHAR(40),
    consentimento_por     VARCHAR(160),
    ativo                 BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            DATETIME(6)  NOT NULL,
    updated_at            DATETIME(6)  NOT NULL,
    created_by            CHAR(36),
    updated_by            CHAR(36),
    deleted               BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_acc_faces_device_user UNIQUE (tenant_id, device_user_id),
    CONSTRAINT fk_acc_faces_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
);
CREATE INDEX idx_acc_faces_titular ON acc_faces(tenant_id, titular_tipo, titular_id, ativo);

-- Veredito por equipamento: o leitor pode RECUSAR a foto (olhos fechados,
-- rosto pequeno, baixa qualidade). Sem isto a tela mente dizendo
-- "sincronizado" quando o leitor nunca aceitou a face.
-- status: PENDENTE | ENVIADA | ACEITA | RECUSADA | REMOVIDA
CREATE TABLE IF NOT EXISTS acc_face_sync (
    id              CHAR(36)    NOT NULL,
    tenant_id       CHAR(36)    NOT NULL,
    face_id         CHAR(36)    NOT NULL,
    dispositivo_id  CHAR(36)    NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    codigo_erro     VARCHAR(40),
    detalhe         VARCHAR(255),
    tentativas      INT         NOT NULL DEFAULT 0,
    foto_hash       VARCHAR(64),
    sincronizado_em DATETIME(6),
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_acc_face_sync UNIQUE (face_id, dispositivo_id),
    CONSTRAINT fk_acc_face_sync_tenant FOREIGN KEY (tenant_id)      REFERENCES tenants(tenant_id),
    CONSTRAINT fk_acc_face_sync_face   FOREIGN KEY (face_id)        REFERENCES acc_faces(id),
    CONSTRAINT fk_acc_face_sync_disp   FOREIGN KEY (dispositivo_id) REFERENCES dispositivos(id)
);
CREATE INDEX idx_acc_face_sync_status ON acc_face_sync(tenant_id, status, updated_at);

-- Sequencia de device_user_id por tenant. Evita o erro classico de usar
-- o id global da pessoa como user_id do leitor: dois tenants que
-- compartilhassem um equipamento corromperiam os dados um do outro.
CREATE TABLE IF NOT EXISTS acc_device_user_seq (
    tenant_id   CHAR(36) NOT NULL,
    proximo_id  BIGINT   NOT NULL DEFAULT 1,
    PRIMARY KEY (tenant_id),
    CONSTRAINT fk_acc_dev_seq_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
);
