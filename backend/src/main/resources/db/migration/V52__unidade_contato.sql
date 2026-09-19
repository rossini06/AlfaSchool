-- =====================================================================
-- CEP, e-mail e telefone da unidade
--
-- O formulario de Escolas coleta os tres e a tabela da tela tem colunas
-- para e-mail e telefone. Nao existia coluna para nenhum: o Jackson
-- descartava os campos desconhecidos, a API respondia 200, e as colunas
-- apareciam sempre como "—".
--
-- Sao dados de contato da UNIDADE, nao da rede: uma escola com duas
-- unidades tem dois telefones, e e' o da unidade que o responsavel liga.
-- =====================================================================

ALTER TABLE units
  ADD COLUMN cep VARCHAR(9) NULL AFTER state,
  ADD COLUMN email VARCHAR(160) NULL AFTER cep,
  ADD COLUMN telefone VARCHAR(20) NULL AFTER email;
