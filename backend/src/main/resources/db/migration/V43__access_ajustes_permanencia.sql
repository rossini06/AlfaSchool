-- =====================================================================
-- AlfaSchool Access — V43: lacunas encontradas ao implementar o motor
--
-- Duas colunas que o schema original nao previu e que a apuracao precisa.
-- =====================================================================

-- O atraso na ENTRADA e' a contrapartida do excedente na saida: a
-- tolerancia de entrada da jornada existe justamente para ele. O motor ja
-- calculava o valor, mas nao havia onde grava-lo, entao o numero era
-- descartado e nao dava para responder "esse aluno chega tarde sempre?".
ALTER TABLE acc_presencas
    ADD COLUMN minutos_atraso INT NOT NULL DEFAULT 0 AFTER minutos_antecipacao;

-- Reabrir uma competencia fechada desfaz o congelamento de um periodo que
-- ja pode ter virado fatura. Sem colunas proprias, a unica trilha ficava
-- em audit_logs; aqui o proprio fechamento carrega quem reabriu e quando.
ALTER TABLE acc_fechamentos
    ADD COLUMN reaberto_por CHAR(36)    NULL AFTER fechado_em,
    ADD COLUMN reaberto_em  DATETIME(6) NULL AFTER reaberto_por,
    ADD COLUMN motivo_reabertura VARCHAR(255) NULL AFTER reaberto_em;
