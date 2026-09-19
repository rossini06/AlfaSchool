-- =====================================================================
-- "Marcar como lido" no portal da familia
--
-- A tela de Avisos tem o botao desde sempre, e um filtro "somente nao
-- lidos". Nao existia nem a coluna nem o endpoint: o botao dava erro a
-- cada clique e o filtro nao filtrava nada.
--
-- A leitura e' do RESPONSAVEL, nao da escola: `lida_em` marca quando a
-- familia abriu, e e' diferente de `entregue_em`, que e' quando o
-- provedor confirmou a entrega da mensagem. Confundir os dois faria a
-- escola achar que a familia leu porque o WhatsApp entregou.
-- =====================================================================

ALTER TABLE acc_notificacao_envios
  ADD COLUMN lida_em DATETIME(6) NULL AFTER entregue_em;

CREATE INDEX idx_acc_envios_titular_lida
    ON acc_notificacao_envios (tenant_id, titular_id, lida_em);
