-- =====================================================================
-- Unicidade que respeita o soft-delete
--
-- O PROBLEMA
-- Todo DELETE do sistema e' logico: a linha fica no banco com deleted=1.
-- Mas nenhum indice unico incluia essa coluna. Consequencia: excluir um
-- registro pela tela queimava a chave para sempre.
--
-- O caso que revelou isso: a escola so' tem dois canais de notificacao
-- (EMAIL e WHATSAPP). Ao excluir a configuracao de e-mail, a linha antiga
-- continuou ocupando (tenant_id, canal), e criar outra passou a responder
-- 409 — para sempre, sem saida pela interface. Um clique legitimo deixava
-- a escola sem canal de aviso ate' alguem mexer no banco.
--
-- A SOLUCAO
-- Uma coluna gerada `vivo`, que vale 1 na linha ativa e NULL na excluida.
-- Em indice unico do MySQL, NULL nunca e' igual a NULL: quantas linhas
-- excluidas se quiser convivem, e so' UMA ativa passa. A regra de negocio
-- continua identica para o dado vivo; o codigo da aplicacao nao muda,
-- porque a coluna e' calculada pelo banco.
--
-- O QUE FICOU DE FORA, DE PROPOSITO
--   acc_painel_dispositivos.token_hash — token revogado nao pode voltar a
--     ser aceito; a unicidade tem que sobreviver a exclusao.
--   tenants.tenant_id e tenants.document — identidade da escola. Recriar
--     um tenant com o mesmo CNPJ enquanto o antigo existe em soft-delete
--     cria duas escolas para o mesmo CNPJ.
-- =====================================================================

ALTER TABLE acc_agent_credenciais
  ADD COLUMN vivo TINYINT GENERATED ALWAYS AS (IF(deleted, NULL, 1)) STORED,
  DROP INDEX uk_acc_agent_cred,
  ADD UNIQUE KEY uk_acc_agent_cred (tenant_id, username, vivo);

ALTER TABLE acc_calendario_dias
  ADD COLUMN vivo TINYINT GENERATED ALWAYS AS (IF(deleted, NULL, 1)) STORED,
  DROP INDEX uk_acc_calendario_dias,
  ADD UNIQUE KEY uk_acc_calendario_dias (calendario_id, data, vivo);

ALTER TABLE acc_faces
  ADD COLUMN vivo TINYINT GENERATED ALWAYS AS (IF(deleted, NULL, 1)) STORED,
  DROP INDEX uk_acc_faces_device_user,
  ADD UNIQUE KEY uk_acc_faces_device_user (tenant_id, device_user_id, vivo);

ALTER TABLE acc_jornada_dias
  ADD COLUMN vivo TINYINT GENERATED ALWAYS AS (IF(deleted, NULL, 1)) STORED,
  DROP INDEX uk_acc_jornada_dias,
  ADD UNIQUE KEY uk_acc_jornada_dias (jornada_id, dia_semana, vivo);

ALTER TABLE acc_jornada_excecoes
  ADD COLUMN vivo TINYINT GENERATED ALWAYS AS (IF(deleted, NULL, 1)) STORED,
  DROP INDEX uk_acc_jornada_excecoes,
  ADD UNIQUE KEY uk_acc_jornada_excecoes (aluno_id, data, vivo);

ALTER TABLE acc_notificacao_configs
  ADD COLUMN vivo TINYINT GENERATED ALWAYS AS (IF(deleted, NULL, 1)) STORED,
  DROP INDEX uk_acc_notif_configs,
  ADD UNIQUE KEY uk_acc_notif_configs (tenant_id, canal, vivo);

ALTER TABLE acc_notificacao_templates
  ADD COLUMN vivo TINYINT GENERATED ALWAYS AS (IF(deleted, NULL, 1)) STORED,
  DROP INDEX uk_acc_notif_templates,
  ADD UNIQUE KEY uk_acc_notif_templates (tenant_id, evento, canal, vivo);

ALTER TABLE acc_paineis
  ADD COLUMN vivo TINYINT GENERATED ALWAYS AS (IF(deleted, NULL, 1)) STORED,
  DROP INDEX uk_acc_paineis_slug,
  ADD UNIQUE KEY uk_acc_paineis_slug (tenant_id, slug, vivo);

ALTER TABLE acc_presencas
  ADD COLUMN vivo TINYINT GENERATED ALWAYS AS (IF(deleted, NULL, 1)) STORED,
  DROP INDEX uk_acc_presencas,
  ADD UNIQUE KEY uk_acc_presencas (aluno_id, data, vivo);

ALTER TABLE conteudos_ministrados
  ADD COLUMN vivo TINYINT GENERATED ALWAYS AS (IF(deleted, NULL, 1)) STORED,
  DROP INDEX uk_conteudo_turma_disc_data,
  ADD UNIQUE KEY uk_conteudo_turma_disc_data (turma_id, disciplina_id, data, tenant_id, vivo);

ALTER TABLE frequencias
  ADD COLUMN vivo TINYINT GENERATED ALWAYS AS (IF(deleted, NULL, 1)) STORED,
  DROP INDEX uk_frequencia,
  ADD UNIQUE KEY uk_frequencia (aluno_id, turma_id, disciplina_id, data, tenant_id, vivo);

ALTER TABLE matriculas
  ADD COLUMN vivo TINYINT GENERATED ALWAYS AS (IF(deleted, NULL, 1)) STORED,
  DROP INDEX uk_matriculas_numero,
  ADD UNIQUE KEY uk_matriculas_numero (numero_matricula, tenant_id, vivo);

ALTER TABLE matriz_curricular
  ADD COLUMN vivo TINYINT GENERATED ALWAYS AS (IF(deleted, NULL, 1)) STORED,
  DROP INDEX uk_matriz_curso_disc_periodo,
  ADD UNIQUE KEY uk_matriz_curso_disc_periodo (curso_id, disciplina_id, periodo, tenant_id, vivo);

ALTER TABLE medias
  ADD COLUMN vivo TINYINT GENERATED ALWAYS AS (IF(deleted, NULL, 1)) STORED,
  DROP INDEX uk_media_matricula_disc_periodo,
  ADD UNIQUE KEY uk_media_matricula_disc_periodo (matricula_id, disciplina_id, periodo, tenant_id, vivo);

ALTER TABLE notas
  ADD COLUMN vivo TINYINT GENERATED ALWAYS AS (IF(deleted, NULL, 1)) STORED,
  DROP INDEX uk_nota_aluno_avaliacao,
  ADD UNIQUE KEY uk_nota_aluno_avaliacao (aluno_id, avaliacao_id, tenant_id, vivo);

ALTER TABLE permissions
  ADD COLUMN vivo TINYINT GENERATED ALWAYS AS (IF(deleted, NULL, 1)) STORED,
  DROP INDEX uk_permissions_tenant_nome,
  ADD UNIQUE KEY uk_permissions_tenant_nome (tenant_id, name, vivo);

ALTER TABLE professor_turma_disciplina
  ADD COLUMN vivo TINYINT GENERATED ALWAYS AS (IF(deleted, NULL, 1)) STORED,
  DROP INDEX uk_ptd,
  ADD UNIQUE KEY uk_ptd (professor_id, turma_id, disciplina_id, tenant_id, vivo);

ALTER TABLE professores
  ADD COLUMN vivo TINYINT GENERATED ALWAYS AS (IF(deleted, NULL, 1)) STORED,
  DROP INDEX uk_professor_cpf_tenant,
  ADD UNIQUE KEY uk_professor_cpf_tenant (cpf, tenant_id, vivo);

ALTER TABLE regras_aprovacao
  ADD COLUMN vivo TINYINT GENERATED ALWAYS AS (IF(deleted, NULL, 1)) STORED,
  DROP INDEX uk_regra_tipo_ensino,
  ADD UNIQUE KEY uk_regra_tipo_ensino (tipo_ensino, tenant_id, vivo);

ALTER TABLE responsaveis
  ADD COLUMN vivo TINYINT GENERATED ALWAYS AS (IF(deleted, NULL, 1)) STORED,
  DROP INDEX uk_responsaveis_user,
  ADD UNIQUE KEY uk_responsaveis_user (user_id, vivo);

ALTER TABLE roles
  ADD COLUMN vivo TINYINT GENERATED ALWAYS AS (IF(deleted, NULL, 1)) STORED,
  DROP INDEX uk_roles_tenant_nome,
  ADD UNIQUE KEY uk_roles_tenant_nome (tenant_id, name, vivo);

ALTER TABLE saas_plans
  ADD COLUMN vivo TINYINT GENERATED ALWAYS AS (IF(deleted, NULL, 1)) STORED,
  DROP INDEX uk_saas_plans_slug,
  ADD UNIQUE KEY uk_saas_plans_slug (slug, tenant_id, vivo);

ALTER TABLE tenant_modulos
  ADD COLUMN vivo TINYINT GENERATED ALWAYS AS (IF(deleted, NULL, 1)) STORED,
  DROP INDEX uk_tenant_modulos,
  ADD UNIQUE KEY uk_tenant_modulos (tenant_id, modulo_codigo, vivo);

ALTER TABLE users
  ADD COLUMN vivo TINYINT GENERATED ALWAYS AS (IF(deleted, NULL, 1)) STORED,
  DROP INDEX uk_users_tenant_email,
  ADD UNIQUE KEY uk_users_tenant_email (tenant_id, email, vivo);

ALTER TABLE usuario_permissao_extra
  ADD COLUMN vivo TINYINT GENERATED ALWAYS AS (IF(deleted, NULL, 1)) STORED,
  DROP INDEX uk_usuario_permissao_extra,
  ADD UNIQUE KEY uk_usuario_permissao_extra (user_id, permissao, vivo);
