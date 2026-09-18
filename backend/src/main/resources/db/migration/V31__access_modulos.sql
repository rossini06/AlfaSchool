-- =====================================================================
-- AlfaSchool Access — V31: modulos licenciaveis
-- Permite que uma escola contrate SOMENTE o controle de acesso, ou a
-- plataforma escolar completa. O guard de modulo (@moduloGuard.has)
-- bloqueia os endpoints de um modulo nao contratado.
-- =====================================================================

CREATE TABLE IF NOT EXISTS modulos (
    id          CHAR(36)     NOT NULL,
    codigo      VARCHAR(40)  NOT NULL,
    nome        VARCHAR(120) NOT NULL,
    descricao   VARCHAR(255),
    ordem       INT          NOT NULL DEFAULT 0,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_modulos_codigo UNIQUE (codigo)
);

CREATE TABLE IF NOT EXISTS tenant_modulos (
    id            CHAR(36)    NOT NULL,
    tenant_id     CHAR(36)    NOT NULL,
    modulo_codigo VARCHAR(40) NOT NULL,
    ativo         BOOLEAN     NOT NULL DEFAULT TRUE,
    ativado_em    DATETIME(6),
    expira_em     DATETIME(6),
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    created_by    CHAR(36),
    updated_by    CHAR(36),
    deleted       BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_tenant_modulos UNIQUE (tenant_id, modulo_codigo),
    CONSTRAINT fk_tenant_modulos_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    CONSTRAINT fk_tenant_modulos_modulo FOREIGN KEY (modulo_codigo) REFERENCES modulos(codigo)
);

CREATE INDEX idx_tenant_modulos_tenant ON tenant_modulos(tenant_id, ativo);

INSERT INTO modulos (id, codigo, nome, descricao, ordem, created_at, updated_at) VALUES
 (UUID(), 'ACCESS',      'Controle de Acesso',  'Entrada, permanencia, retirada segura e paineis ao vivo.', 10, NOW(6), NOW(6)),
 (UUID(), 'PEDAGOGICO',  'Pedagogico',          'Diario de classe, frequencia, avaliacoes, notas e boletim.', 20, NOW(6), NOW(6)),
 (UUID(), 'FINANCEIRO',  'Financeiro',          'Planos, contratos e cobrancas.',                              30, NOW(6), NOW(6)),
 (UUID(), 'PORTAL',      'Portal da Familia',   'Acesso dos responsaveis ao historico e autorizacoes.',        40, NOW(6), NOW(6)),
 (UUID(), 'NOTIFICACOES','Notificacoes',        'Motor de e-mail e WhatsApp.',                                 50, NOW(6), NOW(6));
