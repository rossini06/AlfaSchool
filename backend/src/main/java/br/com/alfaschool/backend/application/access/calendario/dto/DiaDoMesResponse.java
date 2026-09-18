package br.com.alfaschool.backend.application.access.calendario.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Uma casa da grade do mes. `tipo` nulo significa dia sem lancamento, que caiu
 * na regra padrao (util = letivo); a tela pinta esses de forma diferente.
 */
public record DiaDoMesResponse(
        LocalDate data,
        int diaSemana,
        String tipo,
        String descricao,
        boolean letivo,
        boolean cadastrado,
        UUID diaId
) {}
