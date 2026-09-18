package br.com.alfaschool.backend.application.access.portal;

import br.com.alfaschool.backend.application.access.portal.dto.PortalPermanenciaDia;
import br.com.alfaschool.backend.application.access.portal.dto.PortalResumoDia;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Dados de permanencia que o portal mostra a familia.
 *
 * <p>O portal DECLARA esta porta e nao a implementa: permanencia e eventos de
 * acesso pertencem a outra fatia. Assim o portal pode ser escrito, revisado e
 * ter rota liberada antes daquela fatia existir; quando ela chegar, basta
 * registrar um bean que implemente esta interface e o
 * {@link PortalPermanenciaIndisponivel} sai de cena sozinho.
 */
public interface PortalPermanenciaPort {

    PortalResumoDia resumoDoDia(UUID tenantId, UUID alunoId, LocalDate data);

    List<PortalPermanenciaDia> historico(UUID tenantId, UUID alunoId, LocalDate inicio, LocalDate fim);
}
