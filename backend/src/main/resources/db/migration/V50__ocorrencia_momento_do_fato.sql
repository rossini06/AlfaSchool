-- =====================================================================
-- Quando o fato aconteceu, que nao e' quando foi registrado
--
-- O formulario de Ocorrencias pede "Data e hora" como campo obrigatorio
-- ("Informe quando aconteceu") e a tabela da tela tem uma coluna para
-- isso. O campo era enviado, descartado por nao existir no DTO, e a
-- listagem mostrava createdAt — o instante em que alguem digitou.
--
-- A diferenca importa: a coordenacao registra as 17h um episodio das 14h.
-- Gravar 17h desalinha a ocorrencia dos eventos de portaria do mesmo dia,
-- que e' justamente como se investiga o que houve.
--
-- Ocorrencia gerada pelo proprio sistema (tentativa nao autorizada,
-- equipamento offline) tem os dois instantes iguais, e o default cobre as
-- linhas que ja existem.
-- =====================================================================

ALTER TABLE acc_ocorrencias
  ADD COLUMN ocorrido_em DATETIME(6) NULL AFTER status;

UPDATE acc_ocorrencias SET ocorrido_em = created_at WHERE ocorrido_em IS NULL;

CREATE INDEX idx_acc_ocorrencias_ocorrido ON acc_ocorrencias (tenant_id, ocorrido_em);
