-- =====================================================================
-- Revogacao do consentimento de biometria
--
-- O consentimento e' registrado (base legal, data, versao, quem deu) e o
-- sistema recusa exportar a face sem ele. Faltava o outro lado, que a LGPD
-- exige no Art. 8 par. 5: **o consentimento pode ser revogado a qualquer
-- momento, de forma gratuita e facilitada**.
--
-- Nao basta um booleano invertido. Revogar tem consequencia fisica: a face
-- precisa SAIR de cada leitor onde foi gravada, senao a crianca continua
-- entrando pela catraca com um dado que a familia mandou apagar. Por isso
-- guardamos tambem quando e por quem — e' o que a escola apresenta se for
-- questionada.
-- =====================================================================

ALTER TABLE acc_faces
  ADD COLUMN consentimento_revogado_em DATETIME(6) NULL AFTER consentimento_por,
  ADD COLUMN consentimento_revogado_por CHAR(36) NULL AFTER consentimento_revogado_em,
  ADD COLUMN consentimento_revogado_motivo VARCHAR(255) NULL AFTER consentimento_revogado_por;
