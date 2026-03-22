-- Add medical observations field to alunos
ALTER TABLE alunos
    ADD COLUMN observacoes_medicas TEXT NULL;
