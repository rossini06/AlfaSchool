ALTER TABLE avaliacoes
  ADD COLUMN periodo       VARCHAR(20),
  ADD COLUMN nota_minima   DECIMAL(5,2) NOT NULL DEFAULT 5.00,
  ADD COLUMN status        VARCHAR(20)  NOT NULL DEFAULT 'rascunho',
  ADD COLUMN descricao     TEXT,
  ADD COLUMN criterios     TEXT,
  ADD COLUMN data_entrega  DATE;
