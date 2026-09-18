package br.com.alfaschool.backend.application.access.controlid.dto;

import br.com.alfaschool.backend.domain.access.shared.FuncaoDispositivo;
import br.com.alfaschool.backend.domain.access.shared.ModoSync;
import br.com.alfaschool.backend.domain.access.shared.SentidoAcesso;
import br.com.alfaschool.backend.domain.dispositivo.Dispositivo;

import java.time.Instant;
import java.util.UUID;

/**
 * Visao de um equipamento para a tela.
 *
 * NAO carrega senha (nem cifrada) nem o token de webhook: o que nao
 * atravessa a API nao vaza em log de proxy, em cache de navegador nem em
 * print de tela de suporte. "temSenha" e "temWebhookToken" dizem o que a
 * tela precisa saber sem revelar nada.
 */
public record EquipamentoDto(
        UUID id,
        UUID unitId,
        UUID portariaId,
        String nome,
        String tipo,
        String modelo,
        String ip,
        Integer porta,
        String serial,
        FuncaoDispositivo funcao,
        SentidoAcesso sentido,
        ModoSync modoSync,
        Integer grupoAcessoId,
        boolean ativo,
        boolean online,
        boolean sincronizaAuto,
        boolean temSenha,
        boolean temWebhookToken,
        Instant ultimoHeartbeat,
        String agentVersion
) {
    public static EquipamentoDto from(Dispositivo d) {
        return new EquipamentoDto(
                d.getId(),
                d.getUnitId(),
                d.getPortariaId(),
                d.getNome(),
                d.getTipo(),
                d.getModelo(),
                d.getIp(),
                d.getPorta(),
                d.getSerial(),
                d.getFuncao(),
                d.getSentido(),
                d.getModoSync(),
                d.getGrupoAcessoId(),
                d.isAtivo(),
                d.isOnline(),
                d.isSincronizaAuto(),
                d.getSenhaCifrada() != null && d.getSenhaCifrada().length > 0,
                d.getWebhookTokenHash() != null && !d.getWebhookTokenHash().isBlank(),
                d.getUltimoHeartbeat(),
                d.getAgentVersion());
    }
}
