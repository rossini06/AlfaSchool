-- =====================================================================
-- AlfaSchool Access — V44: vinculo real entre usuario e responsavel
--
-- Ate aqui o portal descobria quais alunos o responsavel pode ver
-- casando o E-MAIL do usuario autenticado com responsaveis.email. Isso
-- tem tres defeitos, e o segundo e' um vazamento:
--
--   1. responsavel sem e-mail nao ve nada (falha fechada, tudo bem);
--   2. DOIS responsaveis com o mesmo e-mail — casal que divide caixa —
--      enxergavam os filhos um do outro;
--   3. trocar o e-mail no cadastro mudava, em silencio, o que a pessoa
--      enxerga.
--
-- O vinculo por id resolve os tres. A coluna e' NULA por enquanto: a
-- base existente continua funcionando pelo e-mail ate ser migrada, mas
-- agora o e-mail ambiguo passa a ser RECUSADO em vez de somar os dois.
-- =====================================================================

ALTER TABLE responsaveis
    ADD COLUMN user_id CHAR(36) NULL AFTER tenant_id;

-- Um usuario representa no maximo um responsavel. Varios NULL sao
-- aceitos pelo MySQL, entao a restricao so' vale para quem ja vinculou.
ALTER TABLE responsaveis
    ADD CONSTRAINT uk_responsaveis_user UNIQUE (user_id),
    ADD CONSTRAINT fk_responsaveis_user FOREIGN KEY (user_id) REFERENCES users(id);

CREATE INDEX idx_responsaveis_user ON responsaveis(tenant_id, user_id);

-- Vincula automaticamente o que for inequivoco: e-mail que aponta para
-- exatamente UM usuario e UM responsavel dentro do mesmo tenant. Os
-- ambiguos ficam de fora de proposito, para serem resolvidos a mao — sao
-- justamente os casos que o vazamento afetava.
UPDATE responsaveis r
JOIN (
    SELECT u.id AS user_id, u.tenant_id, LOWER(u.email) AS email
    FROM users u
    WHERE u.email IS NOT NULL AND u.deleted = FALSE
    GROUP BY u.tenant_id, LOWER(u.email), u.id
    HAVING COUNT(*) = 1
) AS uu ON uu.tenant_id = r.tenant_id AND uu.email = LOWER(r.email)
JOIN (
    SELECT tenant_id, LOWER(email) AS email
    FROM responsaveis
    WHERE email IS NOT NULL AND deleted = FALSE
    GROUP BY tenant_id, LOWER(email)
    HAVING COUNT(*) = 1
) AS rr ON rr.tenant_id = r.tenant_id AND rr.email = LOWER(r.email)
SET r.user_id = uu.user_id
WHERE r.deleted = FALSE
  AND r.email IS NOT NULL
  AND r.user_id IS NULL;
