-- =====================================================================
-- Dois campos que a tela coletava e o sistema jogava fora
--
-- O formulario de Pessoas Autorizadas tem um select de parentesco ("Tia",
-- "Vizinha", "Motorista"...) e um campo de observacoes, e a tabela da
-- tela tem uma coluna "Parentesco". Nenhum dos dois existia no banco: o
-- JSON era enviado, o Jackson descartava o campo desconhecido sem erro, a
-- resposta vinha 200, e a coluna aparecia sempre como "—".
--
-- Sao informacoes da PESSOA, nao da autorizacao: e' assim que a portaria
-- reconhece quem esta na frente dela ("a tia do Pedro"). O parentesco por
-- aluno, quando existe vinculo formal, continua em aluno_responsaveis.
-- =====================================================================

ALTER TABLE acc_pessoas_autorizadas
  ADD COLUMN parentesco VARCHAR(40) NULL AFTER nome,
  ADD COLUMN observacoes TEXT NULL AFTER foto_key;
