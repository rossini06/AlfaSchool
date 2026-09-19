-- =====================================================================
-- Numero do processo e orgao emissor da restricao judicial
--
-- A tela de Restricoes Judiciais exige os dois como obrigatorios — "o
-- numero do processo e' o que sustenta o bloqueio" — e os enviava no
-- corpo. Nao existia coluna para nenhum dos dois: o Jackson descartava os
-- campos desconhecidos, e as duas colunas da tabela ficavam sempre
-- vazias. Restava uma medida protetiva sem a referencia do processo que a
-- originou, que e' exatamente o dado que a escola precisa mostrar quando
-- alguem contesta o bloqueio na portaria.
--
-- O `descricao` generico continua existindo e serve as restricoes
-- administrativas, que nao tem processo.
-- =====================================================================

ALTER TABLE acc_restricoes
  ADD COLUMN numero_processo VARCHAR(60) NULL AFTER tipo,
  ADD COLUMN orgao_emissor VARCHAR(120) NULL AFTER numero_processo;
