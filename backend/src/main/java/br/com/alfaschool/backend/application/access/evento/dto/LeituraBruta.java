package br.com.alfaschool.backend.application.access.evento.dto;

import br.com.alfaschool.backend.domain.access.shared.OrigemEvento;
import br.com.alfaschool.backend.domain.access.shared.ResultadoAcesso;
import br.com.alfaschool.backend.domain.access.shared.SentidoAcesso;
import br.com.alfaschool.backend.domain.access.shared.TipoIdentificacao;

import java.time.Instant;
import java.util.UUID;

/**
 * O que chegou do equipamento, antes de qualquer interpretacao.
 *
 * Existe um unico caminho de ingestao — webhook, agente, polling e
 * simulador constroem este record e chamam o mesmo servico. Se o
 * simulador tivesse atalho proprio, ele provaria o funcionamento de um
 * codigo que nao e' o que roda com hardware.
 *
 * @param epochLocalEquipamento segundos contados no relogio LOCAL do
 *        leitor (nao UTC). Preferido sobre dataHora quando presente.
 * @param dataHora usado quando a origem ja resolveu o instante
 *        (simulador, lancamento manual).
 */
public record LeituraBruta(
        UUID dispositivoId,
        Long deviceLogId,
        Long deviceUserId,
        Long epochLocalEquipamento,
        Instant dataHora,
        Integer eventCode,
        ResultadoAcesso resultado,
        TipoIdentificacao tipo,
        SentidoAcesso sentido,
        String motivo,
        OrigemEvento origem,
        String rawJson
) {
}
