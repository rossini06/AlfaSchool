-- =====================================================================
-- O historico de alteracao de nota nunca gravou uma linha
--
-- A entidade HistoricoNota estende BaseEntity, que mapeia created_at,
-- updated_at, created_by, updated_by e deleted. A tabela nao tem nenhuma
-- dessas colunas, entao todo INSERT morre com:
--
--   Unknown column 'created_at' in 'field list'
--
-- E ninguem percebeu porque NotaService.registrarHistorico envolve a
-- gravacao num try/catch que so' loga WARN: a nota e' alterada, o
-- professor ve sucesso, e o rastro da alteracao nao existe. A tabela
-- tinha ZERO linhas.
--
-- Isso e' justamente o registro que a escola precisa quando um
-- responsavel contesta uma nota: quem mudou, de quanto para quanto, e
-- quando.
--
-- Nos testes nao aparecia: eles rodam com ddl-auto=create-drop, que cria
-- a tabela a partir da entidade — e ai ela bate. O defeito so' existia
-- onde o schema vem do Flyway, ou seja, em desenvolvimento e producao.
--
-- Por que acrescentar as colunas em vez de tirar o BaseEntity: e' o
-- BaseEntity que carrega o @FilterDef de tenant. Uma entidade fora dele
-- perderia o isolamento entre escolas, que e' preco alto demais para
-- economizar cinco colunas numa tabela de auditoria.
-- =====================================================================

ALTER TABLE historico_notas
  ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  ADD COLUMN created_by CHAR(36) NULL,
  ADD COLUMN updated_by CHAR(36) NULL,
  ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;

-- A consulta do historico e' sempre por nota, do mais recente para o mais
-- antigo (findByTenantIdAndNotaIdOrderByAlteradoEmDesc), e agora tambem
-- precisa ignorar o que foi excluido logicamente.
--
-- O indice idx_historico_notas_nota, que existe desde antes, NAO pode ser
-- substituido: ele sustenta a chave estrangeira de nota_id, e o InnoDB
-- recusa derruba-lo enquanto for o unico indice liderado por essa coluna.
-- Por isso o novo tem nome proprio e convive com ele.
CREATE INDEX idx_historico_notas_consulta
    ON historico_notas (tenant_id, nota_id, deleted, alterado_em);
