package br.com.alfaschool.backend.application.dispositivo.dto;

import br.com.alfaschool.backend.domain.access.shared.FuncaoDispositivo;
import br.com.alfaschool.backend.domain.access.shared.ModoSync;
import br.com.alfaschool.backend.domain.access.shared.SentidoAcesso;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Cadastro de leitor.
 *
 * Os quatro ultimos campos existem na entidade desde sempre e faltavam
 * aqui: portaria, funcao (quem esse leitor le'), sentido (entrada ou
 * saida) e modo de sincronizacao. Sem eles a tela de Equipamentos de
 * Acesso nao tinha como cadastrar um leitor utilizavel — um leitor sem
 * portaria e sem sentido nao serve nem para a fila de retirada nem para a
 * apuracao de permanencia.
 */
public record DispositivoRequest(
    @NotBlank(message = "Informe o nome do equipamento") @Size(max = 120) String nome,
    @Size(max = 40) String tipo,
    @Size(max = 80) String fabricante,
    @Size(max = 80) String modelo,
    @Size(max = 45) String ip,
    @Min(value = 1, message = "Porta entre 1 e 65535")
    @Max(value = 65535, message = "Porta entre 1 e 65535") Integer porta,
    @Size(max = 80) String serial,
    String apiToken,
    UUID unitId,
    UUID portariaId,
    /** Quem este leitor atende: ALUNO, RESPONSAVEL ou COLABORADOR. */
    FuncaoDispositivo funcao,
    /** ENTRADA, SAIDA ou INDEFINIDO — e' o que define o pareamento do dia. */
    SentidoAcesso sentido,
    ModoSync modoSync,
    Boolean ativo
) {}
