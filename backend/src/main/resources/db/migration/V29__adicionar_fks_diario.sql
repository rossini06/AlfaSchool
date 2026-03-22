-- ============================================
-- V29 - Foreign Keys e Índices para Diário de Classe
-- ============================================

-- 1. FK para frequencias.matricula_id
ALTER TABLE frequencias
    ADD CONSTRAINT fk_frequencias_matricula
    FOREIGN KEY (matricula_id) REFERENCES matriculas(id)
    ON DELETE SET NULL ON UPDATE CASCADE;

-- 2. FK para notas.matricula_id
ALTER TABLE notas
    ADD CONSTRAINT fk_notas_matricula
    FOREIGN KEY (matricula_id) REFERENCES matriculas(id)
    ON DELETE SET NULL ON UPDATE CASCADE;

-- 3. FKs para conteudos_ministrados
ALTER TABLE conteudos_ministrados
    ADD CONSTRAINT fk_conteudos_turma
    FOREIGN KEY (turma_id) REFERENCES turmas(id)
    ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE conteudos_ministrados
    ADD CONSTRAINT fk_conteudos_disciplina
    FOREIGN KEY (disciplina_id) REFERENCES disciplinas(id)
    ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE conteudos_ministrados
    ADD CONSTRAINT fk_conteudos_professor
    FOREIGN KEY (professor_id) REFERENCES professores(id)
    ON DELETE SET NULL ON UPDATE CASCADE;

-- 4. FKs para medias
ALTER TABLE medias
    ADD CONSTRAINT fk_medias_matricula
    FOREIGN KEY (matricula_id) REFERENCES matriculas(id)
    ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE medias
    ADD CONSTRAINT fk_medias_disciplina
    FOREIGN KEY (disciplina_id) REFERENCES disciplinas(id)
    ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE medias
    ADD CONSTRAINT fk_medias_turma
    FOREIGN KEY (turma_id) REFERENCES turmas(id)
    ON DELETE CASCADE ON UPDATE CASCADE;

-- 5. FK para historico_notas
ALTER TABLE historico_notas
    ADD CONSTRAINT fk_historico_nota
    FOREIGN KEY (nota_id) REFERENCES notas(id)
    ON DELETE CASCADE ON UPDATE CASCADE;

-- 6. Índices compostos para melhorar performance de queries frequentes
CREATE INDEX IF NOT EXISTS idx_frequencias_matricula_disciplina
    ON frequencias(matricula_id, disciplina_id);

CREATE INDEX IF NOT EXISTS idx_notas_matricula_avaliacao
    ON notas(matricula_id, avaliacao_id);

CREATE INDEX IF NOT EXISTS idx_medias_matricula_periodo
    ON medias(matricula_id, periodo);

CREATE INDEX IF NOT EXISTS idx_conteudos_turma_disciplina_data
    ON conteudos_ministrados(turma_id, disciplina_id, data);
