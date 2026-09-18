package br.com.alfaschool.backend.application.access.notificacao.dto;

import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import br.com.alfaschool.backend.domain.access.shared.EventoNotificacao;
import br.com.alfaschool.backend.domain.access.shared.TitularTipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * @param habilitado true liga o opt-in e carimba {@code opt_in_em}; false
 *                   carimba {@code opt_out_em}. O carimbo e' a prova do
 *                   consentimento — e' ele que autoriza o envio.
 * @param evento     nulo = preferencia coringa, vale para todos os eventos.
 */
public record PreferenciaRequest(
        @NotNull TitularTipo titularTipo,
        @NotNull UUID titularId,
        @NotNull CanalNotificacao canal,
        EventoNotificacao evento,
        @NotBlank @Size(max = 160) String destino,
        @NotNull Boolean habilitado
) {
}
