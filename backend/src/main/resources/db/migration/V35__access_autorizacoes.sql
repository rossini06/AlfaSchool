-- =====================================================================
-- AlfaSchool Access — V35: quem pode retirar o aluno
--
-- REGRA CENTRAL DE SEGURANCA DESTE MODULO:
-- estar cadastrado como responsavel NAO significa poder retirar, e poder
-- retirar NAO significa poder ver o historico ou receber notificacao.
-- Sao tres permissoes independentes. A avo que busca as sextas nao
-- precisa de login no portal.
--
-- Uma autorizacao temporaria ("a tia busca nesta sexta") jamais deve
-- virar permanente: por isso vigencia, dias e faixa de horario ficam na
-- propria autorizacao, e toda mudanca vai para o historico.
-- =====================================================================

-- Pessoa fisica que pode retirar um aluno. Pode ser um responsavel ja
-- cadastrado (responsavel_id preenchido) ou um terceiro (avo, tia, motorista).
CREATE TABLE IF NOT EXISTS acc_pessoas_autorizadas (
    id              CHAR(36)     NOT NULL,
    tenant_id       CHAR(36)     NOT NULL,
    responsavel_id  CHAR(36),
    nome            VARCHAR(120) NOT NULL,
    cpf             VARCHAR(14),
    rg              VARCHAR(20),
    telefone        VARCHAR(20),
    email           VARCHAR(160),
    foto_key        VARCHAR(255),
    -- permissoes independentes entre si
    pode_retirar        BOOLEAN NOT NULL DEFAULT TRUE,
    pode_acessar_portal BOOLEAN NOT NULL DEFAULT FALSE,
    recebe_notificacao  BOOLEAN NOT NULL DEFAULT FALSE,
    ativo           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    created_by      CHAR(36),
    updated_by      CHAR(36),
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_acc_pessoas_aut_tenant      FOREIGN KEY (tenant_id)      REFERENCES tenants(tenant_id),
    CONSTRAINT fk_acc_pessoas_aut_responsavel FOREIGN KEY (responsavel_id) REFERENCES responsaveis(id)
);
CREATE INDEX idx_acc_pessoas_aut_tenant ON acc_pessoas_autorizadas(tenant_id, ativo);
CREATE INDEX idx_acc_pessoas_aut_cpf    ON acc_pessoas_autorizadas(tenant_id, cpf);

-- status: PENDENTE | ATIVA | SUSPENSA | EXPIRADA | REVOGADA
-- origem: ESCOLA | PORTAL  (pedido pelo portal NAO libera sozinho)
CREATE TABLE IF NOT EXISTS acc_autorizacoes_retirada (
    id                    CHAR(36)    NOT NULL,
    tenant_id             CHAR(36)    NOT NULL,
    aluno_id              CHAR(36)    NOT NULL,
    pessoa_autorizada_id  CHAR(36)    NOT NULL,
    permanente            BOOLEAN     NOT NULL DEFAULT TRUE,
    vigencia_inicio       DATE,
    vigencia_fim          DATE,
    dias_semana           VARCHAR(20),
    hora_inicio           TIME,
    hora_fim              TIME,
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    origem                VARCHAR(20) NOT NULL DEFAULT 'ESCOLA',
    motivo                VARCHAR(255),
    documento_key         VARCHAR(255),
    aprovado_por_user_id  CHAR(36),
    aprovado_em           DATETIME(6),
    observacao            TEXT,
    created_at            DATETIME(6) NOT NULL,
    updated_at            DATETIME(6) NOT NULL,
    created_by            CHAR(36),
    updated_by            CHAR(36),
    deleted               BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_acc_aut_ret_tenant FOREIGN KEY (tenant_id)            REFERENCES tenants(tenant_id),
    CONSTRAINT fk_acc_aut_ret_aluno  FOREIGN KEY (aluno_id)             REFERENCES alunos(id),
    CONSTRAINT fk_acc_aut_ret_pessoa FOREIGN KEY (pessoa_autorizada_id) REFERENCES acc_pessoas_autorizadas(id)
);
CREATE INDEX idx_acc_aut_ret_aluno  ON acc_autorizacoes_retirada(tenant_id, aluno_id, status);
CREATE INDEX idx_acc_aut_ret_pessoa ON acc_autorizacoes_retirada(tenant_id, pessoa_autorizada_id, status);

-- Trilha imutavel: quem criou, aprovou, suspendeu ou revogou, quando e por que.
CREATE TABLE IF NOT EXISTS acc_autorizacao_historico (
    id               CHAR(36)    NOT NULL,
    tenant_id        CHAR(36)    NOT NULL,
    autorizacao_id   CHAR(36)    NOT NULL,
    acao             VARCHAR(30) NOT NULL,
    status_anterior  VARCHAR(20),
    status_novo      VARCHAR(20),
    user_id          CHAR(36),
    motivo           VARCHAR(255),
    ip               VARCHAR(45),
    created_at       DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_acc_aut_hist_tenant FOREIGN KEY (tenant_id)      REFERENCES tenants(tenant_id),
    CONSTRAINT fk_acc_aut_hist_aut    FOREIGN KEY (autorizacao_id) REFERENCES acc_autorizacoes_retirada(id)
);
CREATE INDEX idx_acc_aut_hist ON acc_autorizacao_historico(tenant_id, autorizacao_id, created_at);

-- Restricao judicial / administrativa. Tem precedencia sobre QUALQUER
-- autorizacao: guarda, medida protetiva, divergencia entre responsaveis.
-- O documento fica em storage com acesso restrito.
CREATE TABLE IF NOT EXISTS acc_restricoes (
    id                    CHAR(36)    NOT NULL,
    tenant_id             CHAR(36)    NOT NULL,
    aluno_id              CHAR(36)    NOT NULL,
    pessoa_autorizada_id  CHAR(36),
    pessoa_nome           VARCHAR(120),
    pessoa_cpf            VARCHAR(14),
    tipo                  VARCHAR(20) NOT NULL DEFAULT 'JUDICIAL',
    descricao             TEXT        NOT NULL,
    documento_key         VARCHAR(255),
    vigencia_inicio       DATE,
    vigencia_fim          DATE,
    ativo                 BOOLEAN     NOT NULL DEFAULT TRUE,
    registrado_por_user_id CHAR(36),
    created_at            DATETIME(6) NOT NULL,
    updated_at            DATETIME(6) NOT NULL,
    created_by            CHAR(36),
    updated_by            CHAR(36),
    deleted               BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_acc_restricoes_tenant FOREIGN KEY (tenant_id)            REFERENCES tenants(tenant_id),
    CONSTRAINT fk_acc_restricoes_aluno  FOREIGN KEY (aluno_id)             REFERENCES alunos(id),
    CONSTRAINT fk_acc_restricoes_pessoa FOREIGN KEY (pessoa_autorizada_id) REFERENCES acc_pessoas_autorizadas(id)
);
CREATE INDEX idx_acc_restricoes_aluno ON acc_restricoes(tenant_id, aluno_id, ativo);
CREATE INDEX idx_acc_restricoes_cpf   ON acc_restricoes(tenant_id, pessoa_cpf, ativo);
