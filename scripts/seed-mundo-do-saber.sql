-- =====================================================================
-- Cenario de demonstracao: Colegio Mundo do Saber
--
-- Reproduz a operacao observada na visita: 2 portarias, 3 catracas,
-- 8 leitores faciais Control iD (2 deles exclusivos dos responsaveis),
-- salas com mais de uma turma, e alunos da MESMA turma com jornadas
-- contratadas diferentes.
--
-- NAO e' migration de proposito: dado de demonstracao nao deve entrar
-- em producao por Flyway. Rode a mao:
--   docker exec -i alfaschool-mysql mysql -uroot -palfaschool123 \
--     alfaschool < scripts/seed-mundo-do-saber.sql
--
-- Idempotente: pode rodar quantas vezes quiser.
-- =====================================================================

SET @tenant     = 'b1000000-0000-0000-0000-000000000001';
SET @unidade    = 'b1000000-0000-0000-0000-000000009001';
SET @agora      = NOW(6);

-- ---------------------------------------------------------------- escola
INSERT INTO tenants (id, tenant_id, name, document, active, created_at, updated_at, deleted)
VALUES (@tenant, @tenant, 'Colegio Mundo do Saber', '12345678000199', TRUE, @agora, @agora, FALSE)
ON DUPLICATE KEY UPDATE name = VALUES(name), updated_at = @agora;

INSERT INTO units (id, tenant_id, name, address, city, state, active, created_at, updated_at, deleted)
VALUES (@unidade, @tenant, 'Unidade Principal', 'Rua das Acacias, 100', 'Serra', 'ES', TRUE, @agora, @agora, FALSE)
ON DUPLICATE KEY UPDATE name = VALUES(name), updated_at = @agora;

-- Modulos contratados: a escola do cenario contrata o Access e o Portal,
-- mas NAO o pedagogico — e' exatamente o caso de uso de vender so' o
-- controle de acesso.
INSERT INTO tenant_modulos (id, tenant_id, modulo_codigo, ativo, ativado_em, created_at, updated_at, deleted)
VALUES
 ('b1000000-0000-0000-0000-000000001001', @tenant, 'ACCESS',       TRUE, @agora, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000001002', @tenant, 'PORTAL',       TRUE, @agora, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000001003', @tenant, 'NOTIFICACOES', TRUE, @agora, @agora, @agora, FALSE)
ON DUPLICATE KEY UPDATE ativo = VALUES(ativo), updated_at = @agora;

-- ------------------------------------------------------------- portarias
INSERT INTO acc_portarias (id, tenant_id, unit_id, nome, tipo, descricao, ativo, created_at, updated_at, deleted)
VALUES
 ('b1000000-0000-0000-0000-000000002001', @tenant, @unidade, 'Portaria Principal',   'PRINCIPAL',  'Entrada da Rua das Acacias', TRUE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000002002', @tenant, @unidade, 'Portaria Secundaria',  'SECUNDARIA', 'Acesso lateral / estacionamento', TRUE, @agora, @agora, FALSE)
ON DUPLICATE KEY UPDATE nome = VALUES(nome), updated_at = @agora;

-- ------------------------------------------------------------------ zonas
INSERT INTO acc_zonas (id, tenant_id, unit_id, nome, descricao, ativo, created_at, updated_at, deleted)
VALUES
 ('b1000000-0000-0000-0000-000000003001', @tenant, @unidade, 'Bloco Infantil', 'Salas da educacao infantil', TRUE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000003002', @tenant, @unidade, 'Patio',          'Area de convivencia',        TRUE, @agora, @agora, FALSE)
ON DUPLICATE KEY UPDATE nome = VALUES(nome), updated_at = @agora;

-- ------------------------------------------------------------------ salas
INSERT INTO acc_salas (id, tenant_id, unit_id, zona_id, nome, codigo, bloco, andar, capacidade, ativo, created_at, updated_at, deleted)
VALUES
 ('b1000000-0000-0000-0000-000000004001', @tenant, @unidade, 'b1000000-0000-0000-0000-000000003001', 'Sala 101', '101', 'Infantil', 'Terreo', 25, TRUE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000004002', @tenant, @unidade, 'b1000000-0000-0000-0000-000000003001', 'Sala 102', '102', 'Infantil', 'Terreo', 25, TRUE, @agora, @agora, FALSE)
ON DUPLICATE KEY UPDATE nome = VALUES(nome), updated_at = @agora;

-- ------------------------------------------------------------ curso/turmas
INSERT INTO cursos (id, tenant_id, unit_id, nome, codigo, modalidade, nivel, ativo, created_at, updated_at, deleted)
VALUES ('b1000000-0000-0000-0000-000000005001', @tenant, @unidade, 'Educacao Infantil', 'INF', 'presencial', 'infantil', TRUE, @agora, @agora, FALSE)
ON DUPLICATE KEY UPDATE nome = VALUES(nome), updated_at = @agora;

-- Duas turmas que dividem a MESMA sala 101 em faixas de horario
-- diferentes — o caso que o modelo turma<->sala com vigencia resolve.
INSERT INTO turmas (id, tenant_id, unit_id, curso_id, nome, codigo, ano_letivo, turno, capacidade_maxima, ativa, created_at, updated_at, deleted)
VALUES
 ('b1000000-0000-0000-0000-000000006001', @tenant, @unidade, 'b1000000-0000-0000-0000-000000005001', 'Infantil 2A', 'I2A', 2026, 'manha', 12, TRUE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000006002', @tenant, @unidade, 'b1000000-0000-0000-0000-000000005001', 'Infantil 2B', 'I2B', 2026, 'tarde', 12, TRUE, @agora, @agora, FALSE)
ON DUPLICATE KEY UPDATE nome = VALUES(nome), updated_at = @agora;

INSERT INTO acc_turma_salas (id, tenant_id, turma_id, sala_id, vigencia_inicio, hora_inicio, hora_fim, dias_semana, created_at, updated_at, deleted)
VALUES
 ('b1000000-0000-0000-0000-000000007001', @tenant, 'b1000000-0000-0000-0000-000000006001', 'b1000000-0000-0000-0000-000000004001', '2026-02-01', '07:00:00', '12:30:00', '1,2,3,4,5', @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000007002', @tenant, 'b1000000-0000-0000-0000-000000006002', 'b1000000-0000-0000-0000-000000004001', '2026-02-01', '13:00:00', '18:00:00', '1,2,3,4,5', @agora, @agora, FALSE)
ON DUPLICATE KEY UPDATE updated_at = @agora;

-- --------------------------------------------------------------- jornadas
-- Tres planos contratados distintos. A Ana e o Pedro estao na MESMA
-- turma com jornadas diferentes: e' por isso que jornada e' do aluno.
INSERT INTO acc_jornadas (id, tenant_id, nome, descricao, tolerancia_entrada_min, tolerancia_saida_min, regra_excedente, ativo, created_at, updated_at, deleted)
VALUES
 ('b1000000-0000-0000-0000-000000008001', @tenant, 'Meio periodo manha (5h)', '07:00 as 12:00', 10, 10, 'HORARIO', TRUE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000008002', @tenant, 'Integral (10h)',          '07:00 as 17:00', 10, 15, 'HORARIO', TRUE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000008003', @tenant, 'Meio periodo tarde (5h)', '13:00 as 18:00', 10, 10, 'HORARIO', TRUE, @agora, @agora, FALSE)
ON DUPLICATE KEY UPDATE nome = VALUES(nome), updated_at = @agora;

-- Segunda a sexta (1..5) para cada jornada.
-- INSERT IGNORE: ON DUPLICATE KEY UPDATE com VALUES() nao e' aceito
-- junto de INSERT ... SELECT no MySQL 8.4. A unique (jornada, dia)
-- ja garante a idempotencia.
INSERT IGNORE INTO acc_jornada_dias (id, tenant_id, jornada_id, dia_semana, frequenta, entrada_prevista, saida_prevista, carga_minutos, created_at, updated_at, deleted)
-- O id precisa ter exatamente 36 caracteres: 24 do prefixo mais 12 do
-- ultimo grupo. Uma versao anterior gerava 37, o MySQL truncava e
-- todos os dias da semana colidiam no mesmo id.
SELECT CONCAT('b1000000-0000-0000-0000-', '00000022', j.sufixo, LPAD(d.dia, 2, '0')), @tenant, j.jid, d.dia, TRUE, j.ent, j.sai, j.carga, @agora, @agora, FALSE
FROM (
  SELECT 'b1000000-0000-0000-0000-000000008001' jid, '01' sufixo, '07:00:00' ent, '12:00:00' sai, 300 carga
  UNION ALL SELECT 'b1000000-0000-0000-0000-000000008002', '02', '07:00:00', '17:00:00', 600
  UNION ALL SELECT 'b1000000-0000-0000-0000-000000008003', '03', '13:00:00', '18:00:00', 300
) j
CROSS JOIN (SELECT 1 dia UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) d;

-- ------------------------------------------------------------ calendario
INSERT INTO acc_calendarios (id, tenant_id, unit_id, nome, ano_letivo, ativo, created_at, updated_at, deleted)
VALUES ('b1000000-0000-0000-0000-000000010001', @tenant, @unidade, 'Calendario Letivo 2026', 2026, TRUE, @agora, @agora, FALSE)
ON DUPLICATE KEY UPDATE nome = VALUES(nome), updated_at = @agora;

INSERT INTO acc_calendario_dias (id, tenant_id, calendario_id, data, tipo, descricao, created_at, updated_at, deleted)
VALUES
 ('b1000000-0000-0000-0000-000000010002', @tenant, 'b1000000-0000-0000-0000-000000010001', '2026-09-07', 'FERIADO', 'Independencia', @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000010003', @tenant, 'b1000000-0000-0000-0000-000000010001', '2026-10-12', 'FERIADO', 'Nossa Senhora Aparecida', @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000010004', @tenant, 'b1000000-0000-0000-0000-000000010001', '2026-10-15', 'FACULTATIVO', 'Dia do Professor', @agora, @agora, FALSE)
ON DUPLICATE KEY UPDATE tipo = VALUES(tipo), updated_at = @agora;

-- ----------------------------------------------------------------- alunos
INSERT INTO alunos (id, tenant_id, unit_id, nome, data_nascimento, sexo, ativo, created_at, updated_at, deleted)
VALUES
 ('b1000000-0000-0000-0000-000000012001', @tenant, @unidade, 'Pedro Silva',   '2021-04-12', 'M', TRUE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000012002', @tenant, @unidade, 'Ana Oliveira',  '2021-07-30', 'F', TRUE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000012003', @tenant, @unidade, 'Lucas Santos',  '2021-01-09', 'M', TRUE, @agora, @agora, FALSE)
ON DUPLICATE KEY UPDATE nome = VALUES(nome), updated_at = @agora;

INSERT INTO matriculas (id, tenant_id, unit_id, aluno_id, turma_id, numero_matricula, data_matricula, status, created_at, updated_at, deleted)
VALUES
 ('b1000000-0000-0000-0000-000000013001', @tenant, @unidade, 'b1000000-0000-0000-0000-000000012001', 'b1000000-0000-0000-0000-000000006001', '2026001', '2026-02-01', 'ativa', @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000013002', @tenant, @unidade, 'b1000000-0000-0000-0000-000000012002', 'b1000000-0000-0000-0000-000000006001', '2026002', '2026-02-01', 'ativa', @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000013003', @tenant, @unidade, 'b1000000-0000-0000-0000-000000012003', 'b1000000-0000-0000-0000-000000006002', '2026003', '2026-02-01', 'ativa', @agora, @agora, FALSE)
ON DUPLICATE KEY UPDATE numero_matricula = VALUES(numero_matricula), updated_at = @agora;

-- Pedro fica meio periodo; Ana, da MESMA turma, e' integral.
INSERT INTO acc_aluno_jornadas (id, tenant_id, aluno_id, jornada_id, vigencia_inicio, created_at, updated_at, deleted)
VALUES
 ('b1000000-0000-0000-0000-000000014001', @tenant, 'b1000000-0000-0000-0000-000000012001', 'b1000000-0000-0000-0000-000000008001', '2026-02-01', @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000014002', @tenant, 'b1000000-0000-0000-0000-000000012002', 'b1000000-0000-0000-0000-000000008002', '2026-02-01', @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000014003', @tenant, 'b1000000-0000-0000-0000-000000012003', 'b1000000-0000-0000-0000-000000008003', '2026-02-01', @agora, @agora, FALSE)
ON DUPLICATE KEY UPDATE jornada_id = VALUES(jornada_id), updated_at = @agora;

-- ----------------------------------------------------------- responsaveis
INSERT INTO responsaveis (id, tenant_id, nome, cpf, telefone, whatsapp, email, tipo, principal, created_at, updated_at, deleted)
VALUES
 ('b1000000-0000-0000-0000-000000015001', @tenant, 'Carlos Silva',     '11144477735', '27999990001', '27999990001', 'carlos.silva@exemplo.com',   'financeiro', TRUE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000015002', @tenant, 'Mariana Oliveira', '52998224725', '27999990002', '27999990002', 'mariana.oliveira@exemplo.com','financeiro', TRUE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000015003', @tenant, 'Roberto Santos',   '87748248800', '27999990003', '27999990003', 'roberto.santos@exemplo.com', 'financeiro', TRUE, @agora, @agora, FALSE)
ON DUPLICATE KEY UPDATE nome = VALUES(nome), updated_at = @agora;

INSERT INTO aluno_responsaveis (id, tenant_id, aluno_id, responsavel_id, parentesco, responsavel_financeiro, responsavel_academico, autorizado_buscar, principal, created_at, updated_at, deleted)
VALUES
 ('b1000000-0000-0000-0000-000000016001', @tenant, 'b1000000-0000-0000-0000-000000012001', 'b1000000-0000-0000-0000-000000015001', 'pai',  TRUE, TRUE, TRUE, TRUE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000016002', @tenant, 'b1000000-0000-0000-0000-000000012002', 'b1000000-0000-0000-0000-000000015002', 'mae',  TRUE, TRUE, TRUE, TRUE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000016003', @tenant, 'b1000000-0000-0000-0000-000000012003', 'b1000000-0000-0000-0000-000000015003', 'avo',  TRUE, TRUE, TRUE, TRUE, @agora, @agora, FALSE)
ON DUPLICATE KEY UPDATE parentesco = VALUES(parentesco), updated_at = @agora;

-- Pessoas autorizadas a retirar. Repare na Maria Silva: pode retirar,
-- mas NAO acessa o portal e NAO recebe notificacao — as tres permissoes
-- sao independentes.
INSERT INTO acc_pessoas_autorizadas (id, tenant_id, responsavel_id, nome, cpf, telefone, email, pode_retirar, pode_acessar_portal, recebe_notificacao, ativo, created_at, updated_at, deleted)
VALUES
 ('b1000000-0000-0000-0000-000000017001', @tenant, 'b1000000-0000-0000-0000-000000015001', 'Carlos Silva',     '11144477735', '27999990001', 'carlos.silva@exemplo.com',   TRUE, TRUE,  TRUE,  TRUE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000017002', @tenant, 'b1000000-0000-0000-0000-000000015002', 'Mariana Oliveira', '52998224725', '27999990002', 'mariana.oliveira@exemplo.com',TRUE, TRUE,  TRUE,  TRUE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000017003', @tenant, 'b1000000-0000-0000-0000-000000015003', 'Roberto Santos',   '87748248800', '27999990003', 'roberto.santos@exemplo.com', TRUE, TRUE,  TRUE,  TRUE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000017004', @tenant, NULL, 'Maria Silva (avo)', '39053344705', '27999990004', NULL, TRUE, FALSE, FALSE, TRUE, @agora, @agora, FALSE)
ON DUPLICATE KEY UPDATE nome = VALUES(nome), updated_at = @agora;

-- Autorizacoes: as tres permanentes, mais a avo restrita as sextas-feiras.
INSERT INTO acc_autorizacoes_retirada (id, tenant_id, aluno_id, pessoa_autorizada_id, permanente, vigencia_inicio, vigencia_fim, dias_semana, hora_inicio, hora_fim, status, origem, aprovado_em, created_at, updated_at, deleted)
VALUES
 ('b1000000-0000-0000-0000-000000018001', @tenant, 'b1000000-0000-0000-0000-000000012001', 'b1000000-0000-0000-0000-000000017001', TRUE,  '2026-02-01', NULL,        NULL,  NULL,       NULL,       'ATIVA', 'ESCOLA', @agora, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000018002', @tenant, 'b1000000-0000-0000-0000-000000012002', 'b1000000-0000-0000-0000-000000017002', TRUE,  '2026-02-01', NULL,        NULL,  NULL,       NULL,       'ATIVA', 'ESCOLA', @agora, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000018003', @tenant, 'b1000000-0000-0000-0000-000000012003', 'b1000000-0000-0000-0000-000000017003', TRUE,  '2026-02-01', NULL,        NULL,  NULL,       NULL,       'ATIVA', 'ESCOLA', @agora, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000018004', @tenant, 'b1000000-0000-0000-0000-000000012001', 'b1000000-0000-0000-0000-000000017004', FALSE, '2026-02-01', '2026-12-18','5',   '11:00:00', '13:00:00', 'ATIVA', 'ESCOLA', @agora, @agora, @agora, FALSE)
ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = @agora;

-- ---------------------------------------------------------- equipamentos
-- 8 leitores: 6 de aluno distribuidos em 3 catracas, 2 exclusivos dos
-- responsaveis (um por portaria). Senha fica NULL: o cadastro real deve
-- gravar cifrada pela aplicacao, nunca por SQL.
INSERT INTO dispositivos (id, tenant_id, unit_id, portaria_id, nome, tipo, funcao, sentido, modo_sync, fabricante, modelo, ip, porta, login, grupo_acesso_id, serial, ativo, sincroniza_auto, online, created_at, updated_at, deleted)
VALUES
 ('b1000000-0000-0000-0000-000000019001', @tenant, @unidade, 'b1000000-0000-0000-0000-000000002001', 'Catraca 1 - Entrada', 'catraca', 'ALUNO',       'ENTRADA', 'AGENTE', 'Control iD', 'idface',   '192.168.0.101', 80, 'admin', 1, 'SIM-0001', TRUE, TRUE, FALSE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000019002', @tenant, @unidade, 'b1000000-0000-0000-0000-000000002001', 'Catraca 1 - Saida',   'catraca', 'ALUNO',       'SAIDA',   'AGENTE', 'Control iD', 'idface',   '192.168.0.102', 80, 'admin', 1, 'SIM-0002', TRUE, TRUE, FALSE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000019003', @tenant, @unidade, 'b1000000-0000-0000-0000-000000002001', 'Catraca 2 - Entrada', 'catraca', 'ALUNO',       'ENTRADA', 'AGENTE', 'Control iD', 'idface',   '192.168.0.103', 80, 'admin', 1, 'SIM-0003', TRUE, TRUE, FALSE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000019004', @tenant, @unidade, 'b1000000-0000-0000-0000-000000002001', 'Catraca 2 - Saida',   'catraca', 'ALUNO',       'SAIDA',   'AGENTE', 'Control iD', 'idface',   '192.168.0.104', 80, 'admin', 1, 'SIM-0004', TRUE, TRUE, FALSE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000019005', @tenant, @unidade, 'b1000000-0000-0000-0000-000000002002', 'Catraca 3 - Entrada', 'catraca', 'ALUNO',       'ENTRADA', 'AGENTE', 'Control iD', 'idface',   '192.168.0.105', 80, 'admin', 1, 'SIM-0005', TRUE, TRUE, FALSE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000019006', @tenant, @unidade, 'b1000000-0000-0000-0000-000000002002', 'Catraca 3 - Saida',   'catraca', 'ALUNO',       'SAIDA',   'AGENTE', 'Control iD', 'idface',   '192.168.0.106', 80, 'admin', 1, 'SIM-0006', TRUE, TRUE, FALSE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000019007', @tenant, @unidade, 'b1000000-0000-0000-0000-000000002001', 'Leitor Responsaveis - Principal',  'leitor', 'RESPONSAVEL', 'INDEFINIDO', 'AGENTE', 'Control iD', 'idaccess', '192.168.0.107', 80, 'admin', 1, 'SIM-0007', TRUE, TRUE, FALSE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000019008', @tenant, @unidade, 'b1000000-0000-0000-0000-000000002002', 'Leitor Responsaveis - Secundaria', 'leitor', 'RESPONSAVEL', 'INDEFINIDO', 'AGENTE', 'Control iD', 'idaccess', '192.168.0.108', 80, 'admin', 1, 'SIM-0008', TRUE, TRUE, FALSE, @agora, @agora, FALSE)
ON DUPLICATE KEY UPDATE nome = VALUES(nome), updated_at = @agora;

-- --------------------------------------------------------------- paineis
INSERT INTO acc_paineis (id, tenant_id, unit_id, nome, slug, tipo, exibe_foto, retencao_seg, ativo, created_at, updated_at, deleted)
VALUES
 ('b1000000-0000-0000-0000-000000020001', @tenant, @unidade, 'Central de Coordenacao', 'coordenacao', 'COORDENACAO', TRUE, 20, TRUE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000020002', @tenant, @unidade, 'Painel Infantil 2A',     'infantil-2a', 'SALA',        TRUE, 20, TRUE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000020003', @tenant, @unidade, 'Painel Infantil 2B',     'infantil-2b', 'SALA',        TRUE, 20, TRUE, @agora, @agora, FALSE)
ON DUPLICATE KEY UPDATE nome = VALUES(nome), updated_at = @agora;

INSERT INTO acc_painel_fontes (id, tenant_id, painel_id, escopo, referencia_id, created_at, updated_at)
VALUES
 ('b1000000-0000-0000-0000-000000021001', @tenant, 'b1000000-0000-0000-0000-000000020001', 'UNIDADE', @unidade, @agora, @agora),
 ('b1000000-0000-0000-0000-000000021002', @tenant, 'b1000000-0000-0000-0000-000000020002', 'TURMA',   'b1000000-0000-0000-0000-000000006001', @agora, @agora),
 ('b1000000-0000-0000-0000-000000021003', @tenant, 'b1000000-0000-0000-0000-000000020003', 'TURMA',   'b1000000-0000-0000-0000-000000006002', @agora, @agora)
ON DUPLICATE KEY UPDATE updated_at = @agora;

-- ------------------------------------------------------- usuario da escola
-- Sem um usuario no tenant da escola, nao ha como operar o cenario: o
-- superadmin vive no tenant Master. A senha e' a mesma do superadmin
-- (o hash e' copiado de la), entao NAO use este seed em producao.
INSERT INTO roles (id, tenant_id, name, description, created_at, updated_at, deleted)
VALUES ('b1000000-0000-0000-0000-000000023001', @tenant, 'SUPER_ADMIN', 'Administrador da escola (cenario de demonstracao)', @agora, @agora, FALSE)
ON DUPLICATE KEY UPDATE name = VALUES(name), updated_at = @agora;

INSERT INTO users (id, tenant_id, unit_id, name, email, password, active, locked, failed_attempts, must_change_password, created_at, updated_at, deleted)
SELECT 'b1000000-0000-0000-0000-000000024001', @tenant, @unidade, 'Coordenacao Mundo do Saber',
       'coordenacao@mundodosaber.com', u.password, TRUE, FALSE, 0, FALSE, @agora, @agora, FALSE
FROM users u WHERE u.email = 'superadmin@alfaschool.com' LIMIT 1
ON DUPLICATE KEY UPDATE name = VALUES(name), updated_at = @agora;

INSERT IGNORE INTO user_roles (user_id, role_id)
VALUES ('b1000000-0000-0000-0000-000000024001', 'b1000000-0000-0000-0000-000000023001');

-- ------------------------------------------------------------ biometria
-- O leitor facial identifica a pessoa por um id NUMERICO proprio
-- (device_user_id), nao pelo UUID do sistema. Sem face cadastrada o
-- equipamento nao tem como reconhecer ninguem, e o simulador recusa a
-- simulacao — de proposito, porque e' exatamente o que aconteceria em campo.
--
-- A sequencia e' POR TENANT: usar o id global da pessoa faria duas escolas
-- que compartilhassem um leitor corromperem os dados uma da outra.
--
-- base_legal e consentimento sao obrigatorios para exportar a face ao
-- equipamento. Dado biometrico de crianca e' sensivel com regime reforcado
-- (LGPD Art. 11 e 14): aqui o consentimento e' ficticio, de laboratorio.
INSERT INTO acc_device_user_seq (tenant_id, proximo_id)
VALUES (@tenant, 101)
ON DUPLICATE KEY UPDATE proximo_id = GREATEST(proximo_id, 101);

INSERT INTO acc_faces (id, tenant_id, titular_tipo, titular_id, foto_key, device_user_id,
                       base_legal, consentimento_obtido, consentimento_em, consentimento_versao,
                       consentimento_origem, consentimento_por, ativo, created_at, updated_at, deleted)
VALUES
 ('b1000000-0000-0000-0000-000000025001', @tenant, 'ALUNO',      'b1000000-0000-0000-0000-000000012001', 'demo/aluno-pedro.jpg',  1, 'CONSENTIMENTO_RESPONSAVEL', TRUE, @agora, '1.0', 'SECRETARIA', 'Carlos Silva',     TRUE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000025002', @tenant, 'ALUNO',      'b1000000-0000-0000-0000-000000012002', 'demo/aluno-ana.jpg',    2, 'CONSENTIMENTO_RESPONSAVEL', TRUE, @agora, '1.0', 'SECRETARIA', 'Mariana Oliveira', TRUE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000025003', @tenant, 'ALUNO',      'b1000000-0000-0000-0000-000000012003', 'demo/aluno-lucas.jpg',  3, 'CONSENTIMENTO_RESPONSAVEL', TRUE, @agora, '1.0', 'SECRETARIA', 'Roberto Santos',   TRUE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000025011', @tenant, 'AUTORIZADA', 'b1000000-0000-0000-0000-000000017001', 'demo/resp-carlos.jpg',  11, 'CONSENTIMENTO', TRUE, @agora, '1.0', 'PORTARIA', 'Carlos Silva',     TRUE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000025012', @tenant, 'AUTORIZADA', 'b1000000-0000-0000-0000-000000017002', 'demo/resp-mariana.jpg', 12, 'CONSENTIMENTO', TRUE, @agora, '1.0', 'PORTARIA', 'Mariana Oliveira', TRUE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000025013', @tenant, 'AUTORIZADA', 'b1000000-0000-0000-0000-000000017003', 'demo/resp-roberto.jpg', 13, 'CONSENTIMENTO', TRUE, @agora, '1.0', 'PORTARIA', 'Roberto Santos',   TRUE, @agora, @agora, FALSE),
 ('b1000000-0000-0000-0000-000000025014', @tenant, 'AUTORIZADA', 'b1000000-0000-0000-0000-000000017004', 'demo/resp-maria.jpg',   14, 'CONSENTIMENTO', TRUE, @agora, '1.0', 'PORTARIA', 'Maria Silva',      TRUE, @agora, @agora, FALSE)
ON DUPLICATE KEY UPDATE foto_key = VALUES(foto_key), updated_at = @agora;

-- ------------------------------------------------- usuarios por perfil
-- Um usuario para CADA perfil que uma escola usa, para dar para ver na
-- pratica o que muda de um para o outro. A senha e' a mesma do
-- superadmin (o hash e' copiado de la): cenario de demonstracao, NAO use
-- em producao.
--
-- Os perfis (roles) em si sao criados pelo PermissaoSeeder no boot, com o
-- conjunto de permissoes de cada um. Aqui so' criamos as PESSOAS e as
-- ligamos ao perfil correspondente.
-- INSERT IGNORE: ON DUPLICATE KEY UPDATE com VALUES() nao e' aceito junto
-- de INSERT ... SELECT no MySQL 8.4.
INSERT IGNORE INTO users (id, tenant_id, unit_id, name, email, password, active, locked,
                   failed_attempts, must_change_password, created_at, updated_at, deleted)
SELECT u.novo_id, @tenant, @unidade, u.nome, u.email, base.password,
       TRUE, FALSE, 0, FALSE, @agora, @agora, FALSE
FROM (
            SELECT 'b1000000-0000-0000-0000-000000026001' novo_id, 'Helena Dias (Diretora)'      nome, 'diretor@mundodosaber.com'    email
  UNION ALL SELECT 'b1000000-0000-0000-0000-000000026002',        'Marcia Prado (Coordenação)',        'coordenador@mundodosaber.com'
  UNION ALL SELECT 'b1000000-0000-0000-0000-000000026003',        'Juliana Reis (Secretaria)',         'secretaria@mundodosaber.com'
  UNION ALL SELECT 'b1000000-0000-0000-0000-000000026004',        'Paula Lima (Professora)',           'professor@mundodosaber.com'
  UNION ALL SELECT 'b1000000-0000-0000-0000-000000026005',        'Antonio Souza (Portaria)',          'portaria@mundodosaber.com'
  UNION ALL SELECT 'b1000000-0000-0000-0000-000000026006',        'Rita Alves (Financeiro)',           'financeiro@mundodosaber.com'
  UNION ALL SELECT 'b1000000-0000-0000-0000-000000026007',        'Carlos Silva (Responsável)',        'responsavel@mundodosaber.com'
) u
CROSS JOIN (SELECT password FROM users WHERE email = 'superadmin@alfaschool.com' LIMIT 1) base;

-- Liga cada usuario ao perfil de mesmo nome. Se o perfil ainda nao existe
-- (primeiro boot antes do seeder), o INSERT simplesmente nao encontra a
-- linha e nada e' criado — rodar o seed de novo depois resolve.
INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT m.user_id, r.id
FROM (
            SELECT 'b1000000-0000-0000-0000-000000026001' user_id, 'DIRETOR'     perfil
  UNION ALL SELECT 'b1000000-0000-0000-0000-000000026002',        'COORDENACAO'
  UNION ALL SELECT 'b1000000-0000-0000-0000-000000026003',        'SECRETARIA'
  UNION ALL SELECT 'b1000000-0000-0000-0000-000000026004',        'PROFESSOR'
  UNION ALL SELECT 'b1000000-0000-0000-0000-000000026005',        'PORTARIA'
  UNION ALL SELECT 'b1000000-0000-0000-0000-000000026006',        'FINANCEIRO'
  UNION ALL SELECT 'b1000000-0000-0000-0000-000000026007',        'RESPONSAVEL'
) m
JOIN roles r ON r.tenant_id = @tenant AND r.name = m.perfil AND r.deleted = FALSE;

-- O usuario do responsavel aponta para o cadastro do Carlos Silva, para
-- que o portal mostre os alunos dele — e' o vinculo por id da V44.
UPDATE responsaveis
   SET user_id = 'b1000000-0000-0000-0000-000000026007', updated_at = @agora
 WHERE id = 'b1000000-0000-0000-0000-000000015001';

SELECT 'Cenario Mundo do Saber carregado.' AS status;
SELECT (SELECT COUNT(*) FROM acc_portarias WHERE tenant_id = @tenant) AS portarias,
       (SELECT COUNT(*) FROM dispositivos  WHERE tenant_id = @tenant) AS leitores,
       (SELECT COUNT(*) FROM alunos        WHERE tenant_id = @tenant) AS alunos,
       (SELECT COUNT(*) FROM acc_paineis   WHERE tenant_id = @tenant) AS paineis;
