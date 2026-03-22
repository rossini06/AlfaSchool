-- ============================================
-- V28 - Módulo Diário de Classe
-- ============================================

-- 1. Ajustes na tabela de frequências
-- Adicionar status detalhado e número da aula
ALTER TABLE frequencias
    ADD COLUMN status VARCHAR(20) DEFAULT 'PRESENTE',
    ADD COLUMN numero_aula INT DEFAULT 1,
    ADD COLUMN matricula_id CHAR(36);

-- Atualizar status baseado no campo presente existente
UPDATE frequencias SET status = CASE WHEN presente = true THEN 'PRESENTE' ELSE 'AUSENTE' END WHERE status IS NULL OR status = '';

-- Criar índice para matricula_id
CREATE INDEX idx_frequencias_matricula ON frequencias(matricula_id);
CREATE INDEX idx_frequencias_status ON frequencias(status);

-- 2. Ajustes na tabela de avaliações
-- Adicionar campo para permitir recuperação
ALTER TABLE avaliacoes
    ADD COLUMN permite_recuperacao BOOLEAN DEFAULT TRUE;

-- 3. Ajustes na tabela de notas
-- Adicionar nota de recuperação, nota final e vincular a matrícula
ALTER TABLE notas
    ADD COLUMN nota_recuperacao DECIMAL(5,2),
    ADD COLUMN nota_final DECIMAL(5,2),
    ADD COLUMN matricula_id CHAR(36);

-- Índice para matricula_id em notas
CREATE INDEX idx_notas_matricula ON notas(matricula_id);

-- 4. Nova tabela: Conteúdo Ministrado
CREATE TABLE IF NOT EXISTS conteudos_ministrados (
    id            CHAR(36)     NOT NULL,
    tenant_id     CHAR(36)     NOT NULL,
    turma_id      CHAR(36)     NOT NULL,
    disciplina_id CHAR(36)     NOT NULL,
    professor_id  CHAR(36),
    data          DATE         NOT NULL,
    descricao     TEXT         NOT NULL,
    objetivos     TEXT,
    recursos      VARCHAR(500),
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    created_by    CHAR(36),
    updated_by    CHAR(36),
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_conteudo_turma_disc_data UNIQUE (turma_id, disciplina_id, data, tenant_id)
);

CREATE INDEX idx_conteudos_tenant ON conteudos_ministrados(tenant_id);
CREATE INDEX idx_conteudos_turma ON conteudos_ministrados(turma_id);
CREATE INDEX idx_conteudos_disciplina ON conteudos_ministrados(disciplina_id);
CREATE INDEX idx_conteudos_data ON conteudos_ministrados(data);

-- 5. Nova tabela: Médias
CREATE TABLE IF NOT EXISTS medias (
    id                      CHAR(36)     NOT NULL,
    tenant_id               CHAR(36)     NOT NULL,
    matricula_id            CHAR(36)     NOT NULL,
    disciplina_id           CHAR(36)     NOT NULL,
    turma_id                CHAR(36)     NOT NULL,
    periodo                 VARCHAR(20),
    media                   DECIMAL(5,2),
    percentual_frequencia   DECIMAL(5,2),
    total_aulas             INT          DEFAULT 0,
    total_presencas         INT          DEFAULT 0,
    total_faltas            INT          DEFAULT 0,
    total_justificadas      INT          DEFAULT 0,
    situacao                VARCHAR(20)  DEFAULT 'CURSANDO',
    conceito                VARCHAR(2),
    observacao_descritiva   TEXT,
    created_at              DATETIME(6)  NOT NULL,
    updated_at              DATETIME(6)  NOT NULL,
    created_by              CHAR(36),
    updated_by              CHAR(36),
    deleted                 BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_media_matricula_disc_periodo UNIQUE (matricula_id, disciplina_id, periodo, tenant_id)
);

CREATE INDEX idx_medias_tenant ON medias(tenant_id);
CREATE INDEX idx_medias_matricula ON medias(matricula_id);
CREATE INDEX idx_medias_disciplina ON medias(disciplina_id);
CREATE INDEX idx_medias_turma ON medias(turma_id);
CREATE INDEX idx_medias_situacao ON medias(situacao);

-- 6. Nova tabela: Regras de Aprovação por Tipo de Ensino
CREATE TABLE IF NOT EXISTS regras_aprovacao (
    id                          CHAR(36)     NOT NULL,
    tenant_id                   CHAR(36)     NOT NULL,
    tipo_ensino                 VARCHAR(30)  NOT NULL,
    usa_nota_numerica           BOOLEAN      DEFAULT TRUE,
    usa_conceito                BOOLEAN      DEFAULT FALSE,
    usa_avaliacao_descritiva    BOOLEAN      DEFAULT FALSE,
    nota_minima_aprovacao       DECIMAL(4,2) DEFAULT 6.00,
    frequencia_minima_aprovacao DECIMAL(5,2) DEFAULT 75.00,
    permite_recuperacao         BOOLEAN      DEFAULT TRUE,
    calcula_media_aritmetica    BOOLEAN      DEFAULT TRUE,
    calcula_media_ponderada     BOOLEAN      DEFAULT FALSE,
    exige_projeto_final         BOOLEAN      DEFAULT FALSE,
    aprovacao_por_disciplina    BOOLEAN      DEFAULT FALSE,
    aprovacao_por_modulo        BOOLEAN      DEFAULT FALSE,
    conceitos_possiveis         VARCHAR(50)  DEFAULT 'A,B,C,D,E',
    conceito_minimo_aprovacao   VARCHAR(2)   DEFAULT 'C',
    created_at                  DATETIME(6)  NOT NULL,
    updated_at                  DATETIME(6)  NOT NULL,
    created_by                  CHAR(36),
    updated_by                  CHAR(36),
    deleted                     BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_regra_tipo_ensino UNIQUE (tipo_ensino, tenant_id)
);

CREATE INDEX idx_regras_aprovacao_tenant ON regras_aprovacao(tenant_id);
CREATE INDEX idx_regras_aprovacao_tipo ON regras_aprovacao(tipo_ensino);

-- 7. Dados iniciais das regras de aprovação
INSERT INTO regras_aprovacao (
    id, tenant_id, tipo_ensino,
    usa_nota_numerica, usa_conceito, usa_avaliacao_descritiva,
    nota_minima_aprovacao, frequencia_minima_aprovacao,
    permite_recuperacao, calcula_media_aritmetica, calcula_media_ponderada,
    exige_projeto_final, aprovacao_por_disciplina, aprovacao_por_modulo,
    conceitos_possiveis, conceito_minimo_aprovacao,
    created_at, updated_at, deleted
) VALUES
-- Regra padrão (será usada como template para cada tenant)
('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000', 'INFANTIL',
 FALSE, TRUE, TRUE, NULL, 75.00, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, 'A,B,C', 'C',
 NOW(), NOW(), FALSE),
('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000000', 'FUNDAMENTAL',
 TRUE, FALSE, FALSE, 6.00, 75.00, TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, NULL, NULL,
 NOW(), NOW(), FALSE),
('00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000000', 'MEDIO',
 TRUE, FALSE, FALSE, 6.00, 75.00, TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, NULL, NULL,
 NOW(), NOW(), FALSE),
('00000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000000', 'TECNICO',
 TRUE, FALSE, FALSE, 7.00, 75.00, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, NULL, NULL,
 NOW(), NOW(), FALSE)
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 8. Nova tabela: Histórico de Alterações de Notas (Auditoria)
CREATE TABLE IF NOT EXISTS historico_notas (
    id              CHAR(36)     NOT NULL,
    tenant_id       CHAR(36)     NOT NULL,
    nota_id         CHAR(36)     NOT NULL,
    nota_anterior   DECIMAL(5,2),
    nota_nova       DECIMAL(5,2),
    tipo_alteracao  VARCHAR(30)  NOT NULL,
    motivo          VARCHAR(255),
    alterado_por    CHAR(36)     NOT NULL,
    alterado_em     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_historico_notas_tenant ON historico_notas(tenant_id);
CREATE INDEX idx_historico_notas_nota ON historico_notas(nota_id);
