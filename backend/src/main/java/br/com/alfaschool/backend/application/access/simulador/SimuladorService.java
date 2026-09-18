package br.com.alfaschool.backend.application.access.simulador;

import br.com.alfaschool.backend.application.access.evento.EventoIngestaoService;
import br.com.alfaschool.backend.application.access.evento.RelogioEquipamento;
import br.com.alfaschool.backend.application.access.evento.dto.LeituraBruta;
import br.com.alfaschool.backend.application.access.evento.dto.ResultadoIngestao;
import br.com.alfaschool.backend.application.access.simulador.dto.SimuladorDtos;
import br.com.alfaschool.backend.domain.access.biometria.AccFace;
import br.com.alfaschool.backend.domain.access.evento.AccEvento;
import br.com.alfaschool.backend.domain.access.shared.FuncaoDispositivo;
import br.com.alfaschool.backend.domain.access.shared.OrigemEvento;
import br.com.alfaschool.backend.domain.access.shared.ResultadoAcesso;
import br.com.alfaschool.backend.domain.access.shared.SentidoAcesso;
import br.com.alfaschool.backend.domain.access.shared.TipoIdentificacao;
import br.com.alfaschool.backend.domain.access.shared.TitularTipo;
import br.com.alfaschool.backend.domain.dispositivo.Dispositivo;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccEventoRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccFaceRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.DispositivoRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Gerador de cenarios de leitura, para desenvolver e testar sem hardware.
 *
 * Todos os cenarios passam pelo MESMO EventoIngestaoService que o
 * hardware usa. Isso e' a regra que da valor ao simulador: se ele
 * escrevesse direto em acc_eventos, validaria um caminho que nao existe
 * em producao, e a deduplicacao, a conversao de fuso e a resolucao de
 * titular nunca seriam exercitadas.
 *
 * Nunca em producao: @ConditionalOnProperty em
 * app.access.simulador-habilitado, false por padrao. Um endpoint que
 * fabrica entrada de aluno e' falsificacao de registro de frequencia.
 */
@Service
@ConditionalOnProperty(name = "app.access.simulador-habilitado", havingValue = "true")
public class SimuladorService {

    private final EventoIngestaoService ingestao;
    private final AccFaceRepository faces;
    private final AccEventoRepository eventos;
    private final DispositivoRepository dispositivos;

    /**
     * Contador de device_log_id por dispositivo, imitando o contador
     * interno do equipamento. Comeca no numero de eventos ja gravados
     * para nao colidir com o historico do banco de laboratorio.
     */
    private final Map<UUID, AtomicLong> contadores = new ConcurrentHashMap<>();

    public SimuladorService(EventoIngestaoService ingestao,
                            AccFaceRepository faces,
                            AccEventoRepository eventos,
                            DispositivoRepository dispositivos) {
        this.ingestao = ingestao;
        this.faces = faces;
        this.eventos = eventos;
        this.dispositivos = dispositivos;
    }

    // =================================================================
    // Cenarios individuais
    // =================================================================

    public SimuladorDtos.EventoGeradoDto entradaAluno(UUID tenantId, UUID alunoId,
                                                      UUID dispositivoId, Instant quando) {
        AccFace face = faceDe(tenantId, TitularTipo.ALUNO, alunoId,
                "Aluno sem biometria cadastrada: cadastre a face antes de simular a entrada.");
        return disparar(tenantId, dispositivoId, face.getDeviceUserId(), quando,
                ResultadoAcesso.PERMITIDO, SentidoAcesso.ENTRADA, null, "entrada-aluno", 7);
    }

    public SimuladorDtos.EventoGeradoDto saidaAluno(UUID tenantId, UUID alunoId,
                                                    UUID dispositivoId, Instant quando) {
        AccFace face = faceDe(tenantId, TitularTipo.ALUNO, alunoId,
                "Aluno sem biometria cadastrada: cadastre a face antes de simular a saída.");
        return disparar(tenantId, dispositivoId, face.getDeviceUserId(), quando,
                ResultadoAcesso.PERMITIDO, SentidoAcesso.SAIDA, null, "saida-aluno", 7);
    }

    /**
     * Chegada de responsavel/pessoa autorizada.
     *
     * Reconhecer o responsavel NAO entrega o aluno: este evento so' abre
     * a fila de retirada. Quem interpreta isso e' o consumidor do
     * AcessoRegistradoEvent, e o simulador nao atalha nada disso.
     */
    public SimuladorDtos.EventoGeradoDto chegadaResponsavel(UUID tenantId, UUID pessoaAutorizadaId,
                                                            UUID dispositivoId, Instant quando) {
        AccFace face = faces
                .findByTenantIdAndTitularTipoAndTitularIdAndDeletedFalse(
                        tenantId, TitularTipo.AUTORIZADA, pessoaAutorizadaId)
                .or(() -> faces.findByTenantIdAndTitularTipoAndTitularIdAndDeletedFalse(
                        tenantId, TitularTipo.RESPONSAVEL, pessoaAutorizadaId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Pessoa autorizada sem biometria cadastrada."));
        return disparar(tenantId, dispositivoId, face.getDeviceUserId(), quando,
                ResultadoAcesso.PERMITIDO, SentidoAcesso.ENTRADA, null,
                "chegada-responsavel", 7);
    }

    /** Tentativa negada de alguem que EXISTE no equipamento (fora de horario, sem permissao). */
    public SimuladorDtos.EventoGeradoDto acessoNegado(UUID tenantId, Long deviceUserId,
                                                      UUID dispositivoId, String motivo) {
        return disparar(tenantId, dispositivoId, deviceUserId, null,
                ResultadoAcesso.NEGADO, null,
                motivo == null ? "Acesso negado pela regra do equipamento." : motivo,
                "acesso-negado", 6);
    }

    /**
     * Pessoa que o leitor nao reconheceu: user_id = 0 no firmware.
     *
     * O evento e' gravado com titular DESCONHECIDO e titular_id NULL —
     * exatamente o comportamento que nao pode regredir para "inventar um
     * id porque o numero existia".
     */
    public SimuladorDtos.EventoGeradoDto pessoaDesconhecida(UUID tenantId, UUID dispositivoId) {
        return disparar(tenantId, dispositivoId, 0L, null,
                ResultadoAcesso.DESCONHECIDO, null,
                "Pessoa não identificada pelo leitor.", "pessoa-desconhecida", 8);
    }

    /**
     * Reenvia um evento ja gravado com o MESMO device_log_id e o MESMO
     * horario — o que a fila offline do agente faz quando o backend
     * responde tarde.
     *
     * Serve de prova viva da deduplicacao: o resultado tem de vir com
     * replay=true e o mesmo eventoId, sem gravar linha nova.
     */
    public SimuladorDtos.EventoGeradoDto eventoDuplicado(UUID tenantId, UUID eventoId) {
        AccEvento original = eventos.findByIdAndTenantId(eventoId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Evento não encontrado."));
        if (original.getDeviceLogId() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Evento sem device_log_id não pode ser deduplicado.");
        }
        LeituraBruta leitura = new LeituraBruta(
                original.getDispositivoId(),
                original.getDeviceLogId(),
                original.getDeviceUserId(),
                RelogioEquipamento.paraEpochLocal(original.getDataHora(), ingestao.zonaDoEquipamento()),
                null,
                null,
                original.getResultado(),
                original.getTipo(),
                original.getSentido(),
                original.getMotivo(),
                OrigemEvento.SIMULADOR,
                original.getRawJson());
        ResultadoIngestao r = ingestao.registrar(tenantId, leitura);
        return new SimuladorDtos.EventoGeradoDto(r.eventoId(), r.replay(),
                original.getDeviceLogId(), original.getDeviceUserId(),
                original.getDataHora(), "evento-duplicado");
    }

    // =================================================================
    // Rotina de um dia inteiro
    // =================================================================

    /**
     * Simula um dia letivo: entradas de manha, chegada de responsaveis a
     * tarde, saidas em seguida.
     *
     * Os horarios sao espacados em minutos porque a dedup usa janela de
     * tempo: disparar tudo no mesmo instante faria leituras distintas
     * parecerem replay uma da outra e o cenario nao provaria nada.
     */
    public SimuladorDtos.RotinaDiaDto rotinaDia(UUID tenantId, UUID unitId, Integer maximoAlunos) {
        List<Dispositivo> daUnidade = dispositivos
                .findByTenantIdAndDeletedFalse(tenantId, Pageable.unpaged())
                .getContent().stream()
                .filter(d -> unitId.equals(d.getUnitId()) && d.isAtivo())
                .toList();
        if (daUnidade.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Nenhum equipamento ativo nesta unidade.");
        }

        Dispositivo entrada = escolher(daUnidade, FuncaoDispositivo.ALUNO, SentidoAcesso.ENTRADA);
        Dispositivo saida = escolher(daUnidade, FuncaoDispositivo.ALUNO, SentidoAcesso.SAIDA);
        Dispositivo leitorDosPais = daUnidade.stream()
                .filter(d -> d.getFuncao() == FuncaoDispositivo.RESPONSAVEL)
                .findFirst().orElse(entrada);

        List<AccFace> todas = faces.findByTenantIdAndAtivoTrueAndDeletedFalse(tenantId);
        List<AccFace> alunos = todas.stream()
                .filter(f -> f.getTitularTipo() == TitularTipo.ALUNO)
                .limit(maximoAlunos == null ? 20 : maximoAlunos)
                .toList();
        List<AccFace> responsaveis = todas.stream()
                .filter(f -> f.getTitularTipo() == TitularTipo.RESPONSAVEL
                        || f.getTitularTipo() == TitularTipo.AUTORIZADA)
                .limit(10)
                .toList();

        LocalDate hoje = LocalDate.now(ingestao.zonaDoEquipamento());
        List<SimuladorDtos.EventoGeradoDto> gerados = new ArrayList<>();
        List<String> avisos = new ArrayList<>();

        if (alunos.isEmpty()) {
            avisos.add("Nenhuma face de aluno cadastrada: a rotina não gerou entradas.");
        }

        int i = 0;
        for (AccFace f : alunos) {
            Instant quando = hoje.atTime(LocalTime.of(7, 20).plusMinutes(i))
                    .atZone(ingestao.zonaDoEquipamento()).toInstant();
            gerados.add(disparar(tenantId, entrada.getId(), f.getDeviceUserId(), quando,
                    ResultadoAcesso.PERMITIDO, SentidoAcesso.ENTRADA, null, "rotina-entrada", 7));
            i++;
        }
        int entradas = i;

        i = 0;
        for (AccFace f : responsaveis) {
            Instant quando = hoje.atTime(LocalTime.of(17, 0).plusMinutes(i))
                    .atZone(ingestao.zonaDoEquipamento()).toInstant();
            gerados.add(disparar(tenantId, leitorDosPais.getId(), f.getDeviceUserId(), quando,
                    ResultadoAcesso.PERMITIDO, SentidoAcesso.ENTRADA, null,
                    "rotina-chegada-responsavel", 7));
            i++;
        }
        int chegadas = i;

        i = 0;
        for (AccFace f : alunos) {
            Instant quando = hoje.atTime(LocalTime.of(17, 30).plusMinutes(i))
                    .atZone(ingestao.zonaDoEquipamento()).toInstant();
            gerados.add(disparar(tenantId, saida.getId(), f.getDeviceUserId(), quando,
                    ResultadoAcesso.PERMITIDO, SentidoAcesso.SAIDA, null, "rotina-saida", 7));
            i++;
        }

        if (responsaveis.isEmpty()) {
            avisos.add("Nenhuma face de responsável/pessoa autorizada cadastrada.");
        }
        if (saida.getId().equals(entrada.getId())) {
            avisos.add("A unidade não tem leitor de saída dedicado: entradas e saídas "
                    + "foram geradas no mesmo equipamento.");
        }

        return new SimuladorDtos.RotinaDiaDto(entradas, chegadas, i, gerados, avisos);
    }

    // =================================================================
    // Apoio
    // =================================================================

    private SimuladorDtos.EventoGeradoDto disparar(UUID tenantId, UUID dispositivoId,
                                                   Long deviceUserId, Instant quando,
                                                   ResultadoAcesso resultado, SentidoAcesso sentido,
                                                   String motivo, String cenario, int eventCode) {
        Instant momento = quando == null ? Instant.now() : quando;
        long logId = proximoLogId(tenantId, dispositivoId);

        LeituraBruta leitura = new LeituraBruta(
                dispositivoId,
                logId,
                deviceUserId,
                // Converte para o epoch "local" que o firmware produziria,
                // para que a leitura percorra a mesma conversao de fuso do
                // caminho real em vez de pular direto para um Instant.
                RelogioEquipamento.paraEpochLocal(momento, ingestao.zonaDoEquipamento()),
                null,
                eventCode,
                resultado,
                TipoIdentificacao.FACE,
                sentido,
                motivo,
                OrigemEvento.SIMULADOR,
                "{\"simulador\":\"" + cenario + "\"}");

        ResultadoIngestao r = ingestao.registrar(tenantId, leitura);
        return new SimuladorDtos.EventoGeradoDto(r.eventoId(), r.replay(), logId,
                deviceUserId, momento, cenario);
    }

    private long proximoLogId(UUID tenantId, UUID dispositivoId) {
        AtomicLong contador = contadores.computeIfAbsent(dispositivoId,
                id -> new AtomicLong(eventos.countByTenantIdAndDispositivoId(tenantId, id) + 1));
        return contador.getAndIncrement();
    }

    private AccFace faceDe(UUID tenantId, TitularTipo tipo, UUID titularId, String erro) {
        return faces.findByTenantIdAndTitularTipoAndTitularIdAndDeletedFalse(tenantId, tipo, titularId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, erro));
    }

    private Dispositivo escolher(List<Dispositivo> candidatos, FuncaoDispositivo funcao,
                                 SentidoAcesso sentido) {
        return candidatos.stream()
                .filter(d -> d.getFuncao() == funcao && d.getSentido() == sentido)
                .findFirst()
                .orElseGet(() -> candidatos.stream()
                        .filter(d -> d.getFuncao() != FuncaoDispositivo.RESPONSAVEL)
                        .findFirst()
                        .orElse(candidatos.get(0)));
    }
}
