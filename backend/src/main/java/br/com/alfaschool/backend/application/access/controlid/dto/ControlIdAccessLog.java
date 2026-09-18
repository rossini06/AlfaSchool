package br.com.alfaschool.backend.application.access.controlid.dto;

/**
 * Linha de access_logs lida do equipamento.
 *
 * time NAO e' epoch UTC: o firmware calcula os segundos desde 1970 usando
 * o relogio LOCAL do aparelho. A conversao correta vive em
 * {@code EventoIngestaoService.instanteDoEquipamento}.
 *
 * userId = 0 significa "nao identificado" — e' um evento legitimo, nao um
 * erro de leitura, e precisa ser gravado como DESCONHECIDO.
 */
public record ControlIdAccessLog(
        Long id,
        Long userId,
        Long time,
        Integer event,
        Integer deviceId,
        Integer portalId,
        String uhfTag
) {
    /** Codigos de identificacao por face/cartao/digital que significam acesso concedido. */
    public boolean identificado() {
        return userId != null && userId > 0;
    }
}
