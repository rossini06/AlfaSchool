-- =====================================================================
-- AlfaSchool — V45: permissoes efetivas
--
-- As tabelas roles/permissions/role_permissions existem desde a V1, mas
-- estavam VAZIAS: nenhuma permissao cadastrada, nenhum vinculo. Na
-- pratica nao havia controle — o menu mostrava tudo para todos e os
-- @PreAuthorize citavam perfis inexistentes (COORDENACAO, SECRETARIA),
-- que nunca casavam com nada.
--
-- Esta migration nao semeia o catalogo: quem faz isso e' o
-- PermissaoSeeder, no boot, para que uma escola criada amanha nasca com
-- os perfis certos sem depender de migration nova. Aqui ficam so' as
-- estruturas que faltavam.
-- =====================================================================

-- Permissao concedida a UMA pessoa, alem do que o perfil dela da'.
--
-- E' somente ADITIVA de proposito: nao existe "negar". Regra de negacao
-- espalhada por usuario vira labirinto — ninguem descobre por que fulano
-- nao consegue fazer algo. Para tirar acesso, troca-se o perfil.
CREATE TABLE IF NOT EXISTS usuario_permissao_extra (
    id           CHAR(36)    NOT NULL,
    tenant_id    CHAR(36)    NOT NULL,
    user_id      CHAR(36)    NOT NULL,
    permissao    VARCHAR(60) NOT NULL,
    motivo       VARCHAR(255),
    concedido_por CHAR(36),
    concedido_em DATETIME(6) NOT NULL,
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    created_by   CHAR(36),
    updated_by   CHAR(36),
    deleted      BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_usuario_permissao_extra UNIQUE (user_id, permissao),
    CONSTRAINT fk_upe_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    CONSTRAINT fk_upe_user   FOREIGN KEY (user_id)   REFERENCES users(id)
);
CREATE INDEX idx_upe_user ON usuario_permissao_extra(tenant_id, user_id);

-- Identifica o perfil de sistema (DIRETOR, COORDENACAO, ...) e o protege
-- de exclusao. O perfil que a escola cria por conta propria fica com
-- sistema = FALSE e pode ser removido a vontade.
ALTER TABLE roles
    ADD COLUMN sistema BOOLEAN NOT NULL DEFAULT FALSE AFTER description,
    ADD COLUMN rotulo VARCHAR(80) NULL AFTER name;

-- Um papel com o mesmo nome duas vezes no mesmo tenant tornaria a
-- resolucao de permissao dependente da ordem de leitura.
ALTER TABLE roles
    ADD CONSTRAINT uk_roles_tenant_nome UNIQUE (tenant_id, name);

ALTER TABLE permissions
    ADD CONSTRAINT uk_permissions_tenant_nome UNIQUE (tenant_id, name);
