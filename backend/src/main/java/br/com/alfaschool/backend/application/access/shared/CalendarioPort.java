package br.com.alfaschool.backend.application.access.shared;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Calendario letivo. Separado da jornada de proposito: o calendario diz se
 * a ESCOLA abre; a jornada contratada diz se AQUELE aluno deveria estar la.
 */
public interface CalendarioPort {

    boolean ehDiaLetivo(UUID tenantId, UUID unitId, LocalDate data);
}
