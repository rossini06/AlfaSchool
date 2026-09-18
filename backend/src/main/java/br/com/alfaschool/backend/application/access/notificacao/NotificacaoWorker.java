package br.com.alfaschool.backend.application.access.notificacao;

import br.com.alfaschool.backend.application.access.notificacao.sender.EnvioRequest;
import br.com.alfaschool.backend.application.access.notificacao.sender.NotificacaoSender;
import br.com.alfaschool.backend.application.access.notificacao.sender.ResultadoEnvio;
import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoConfig;
import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoEnvio;
import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import br.com.alfaschool.backend.domain.access.shared.StatusEnvio;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccNotificacaoConfigRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccNotificacaoEnvioRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccNotificacaoTemplateRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Processa a fila de envios.
 *
 * <h2>Decisoes que existem por causa de producao</h2>
 * <ul>
 *   <li><b>Claim atomico</b> antes de qualquer chamada ao provedor. Sem ele,
 *       duas instancias do backend leem a mesma linha PENDENTE e a familia
 *       recebe a mensagem duas vezes.</li>
 *   <li><b>Backoff exponencial</b> 1min, 5min, 15min, 1h. Reenviar em loop
 *       apertado contra um provedor com rate limit so' aprofunda o buraco.</li>
 *   <li><b>Expiracao em 24h.</b> "Seu filho entrou na escola" entregue no dia
 *       seguinte e' pior do que nao entregue: assusta a familia.</li>
 *   <li><b>Teto diario por tenant e canal.</b> No pico de saida centenas de
 *       avisos saem em minutos; sem teto a cota do provedor estoura e o resto
 *       do dia e' recusado. Ao bater o teto reagendamos, nao falhamos.</li>
 *   <li><b>Erro permanente nao volta.</b> Endereco invalido nao melhora com
 *       insistencia; insistir queima reputacao do remetente.</li>
 * </ul>
 */
@Component
public class NotificacaoWorker {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoWorker.class);

    /** Fuso das escolas. O teto diario e o corte de "hoje" seguem o dia local. */
    static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    /** Backoff por tentativa ja realizada: 1min, 5min, 15min, 1h. */
    static final Duration[] BACKOFF = {
            Duration.ofMinutes(1),
            Duration.ofMinutes(5),
            Duration.ofMinutes(15),
            Duration.ofHours(1)
    };

    /**
     * Teto de tentativas. E' constante porque a V42 nao tem coluna
     * {@code max_tentativas} em {@code acc_notificacao_envios}. Ver relatorio.
     */
    public static final int MAX_TENTATIVAS = 4;

    /** Janela util de um aviso de acesso. Passou disso, nao serve mais. */
    static final Duration JANELA_UTIL = Duration.ofHours(24);

    /** Quantos envios a varredura pega por rodada. */
    private static final int LOTE = 200;

    private final AccNotificacaoEnvioRepository envioRepository;
    private final AccNotificacaoConfigRepository configRepository;
    private final AccNotificacaoTemplateRepository templateRepository;
    private final EnvioFilaWriter filaWriter;
    private final Map<CanalNotificacao, NotificacaoSender> senders = new EnumMap<>(CanalNotificacao.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NotificacaoWorker(AccNotificacaoEnvioRepository envioRepository,
                             AccNotificacaoConfigRepository configRepository,
                             AccNotificacaoTemplateRepository templateRepository,
                             EnvioFilaWriter filaWriter,
                             List<NotificacaoSender> sendersDisponiveis) {
        this.envioRepository = envioRepository;
        this.configRepository = configRepository;
        this.templateRepository = templateRepository;
        this.filaWriter = filaWriter;
        for (NotificacaoSender s : sendersDisponiveis) {
            this.senders.put(s.canal(), s);
        }
    }

    /**
     * Processa um envio no pool dedicado, para que um provedor lento (SMTP
     * travado, timeout da Meta) nao segure as threads que atendem a portaria.
     */
    @Async("notificacaoExecutor")
    public void processar(UUID envioId) {
        try {
            processarSincrono(envioId);
        } catch (RuntimeException e) {
            log.error("Falha nao tratada ao processar envio {}: {}", envioId, e.toString(), e);
        }
    }

    /** Mesma logica sem o pool — usada pela varredura e pelos testes. */
    public void processarSincrono(UUID envioId) {
        Optional<AccNotificacaoEnvio> reivindicado = filaWriter.reivindicar(envioId);
        if (reivindicado.isEmpty()) {
            // Outro worker pegou esta linha. Abortar em silencio e' o certo.
            log.debug("Envio {} ja reivindicado por outro worker", envioId);
            return;
        }
        AccNotificacaoEnvio envio = reivindicado.get();

        // Expiracao checada depois do claim, para nao competir com o envio.
        if (envio.getCreatedAt() != null && Instant.now().isAfter(envio.getCreatedAt().plus(JANELA_UTIL))) {
            log.warn("Envio {} expirado: passou da janela util de {}h", envio.getId(), JANELA_UTIL.toHours());
            filaWriter.marcarExpirado(envio.getId());
            return;
        }

        AccNotificacaoConfig config = configRepository
                .findFirstByTenantIdAndCanalAndAtivoTrueAndDeletedFalse(envio.getTenantId(), envio.getCanal())
                .orElse(null);

        if (tetoDiarioAtingido(envio, config)) {
            Instant amanha = inicioDoProximoDia();
            log.warn("Teto diario do canal {} atingido para tenant {} — envio {} reagendado para {}",
                    envio.getCanal(), envio.getTenantId(), envio.getId(), amanha);
            filaWriter.reagendarSemPenalidade(envio.getId(), amanha, "TETO_DIARIO");
            return;
        }

        NotificacaoSender sender = senders.get(envio.getCanal());
        if (sender == null) {
            // Canal sem implementacao: repetir nao resolve.
            filaWriter.registrarFalha(envio.getId(), "CANAL_SEM_SENDER",
                    "Nenhum NotificacaoSender registrado para " + envio.getCanal(), null);
            return;
        }

        ResultadoEnvio resultado;
        try {
            resultado = sender.enviar(new EnvioRequest(
                    envio.getId(), envio.getTenantId(), envio.getDestino(),
                    envio.getAssunto(), envio.getCorpo(),
                    templateExterno(envio), variaveis(envio), config));
        } catch (RuntimeException e) {
            // Contrato do sender e' nao lancar; se lancar, tratamos como
            // transitorio: melhor tentar de novo do que descartar um aviso.
            resultado = ResultadoEnvio.transitorio("ERRO_SENDER", e.toString());
        }

        aplicarResultado(envio, resultado);
    }

    void aplicarResultado(AccNotificacaoEnvio envio, ResultadoEnvio resultado) {
        if (resultado.sucesso()) {
            filaWriter.registrarSucesso(envio.getId(), resultado.providerMessageId());
            return;
        }

        if (resultado.permanente()) {
            // agendadoPara null = fora da fila para sempre.
            filaWriter.registrarFalha(envio.getId(), resultado.codigoErro(), resultado.mensagemErro(), null);
            return;
        }

        int tentativasFeitas = envio.getTentativas() + 1;
        if (tentativasFeitas >= MAX_TENTATIVAS) {
            filaWriter.registrarFalha(envio.getId(), resultado.codigoErro(),
                    "Teto de tentativas atingido: " + resultado.mensagemErro(), null);
            return;
        }

        Instant proxima = Instant.now().plus(backoffPara(tentativasFeitas));
        Instant limite = envio.getCreatedAt() == null
                ? Instant.now().plus(JANELA_UTIL)
                : envio.getCreatedAt().plus(JANELA_UTIL);
        if (proxima.isAfter(limite)) {
            // A proxima tentativa cairia fora da janela util: nao vale a pena.
            log.warn("Envio {} expirado: proxima tentativa cairia fora da janela util", envio.getId());
            filaWriter.marcarExpirado(envio.getId());
            return;
        }
        filaWriter.registrarFalha(envio.getId(), resultado.codigoErro(), resultado.mensagemErro(), proxima);
    }

    /** Backoff pela tentativa ja realizada; o ultimo degrau se repete. */
    public static Duration backoffPara(int tentativasFeitas) {
        int indice = Math.max(0, Math.min(tentativasFeitas - 1, BACKOFF.length - 1));
        return BACKOFF[indice];
    }

    /**
     * Nome do template aprovado no provedor externo. Nao ha coluna para isso em
     * {@code acc_notificacao_envios}, entao lemos do cadastro no momento do
     * envio — que e' onde a Meta valida esse nome de qualquer forma.
     */
    private String templateExterno(AccNotificacaoEnvio envio) {
        if (envio.getCanal() != CanalNotificacao.WHATSAPP) {
            return null;
        }
        return templateRepository
                .findFirstByTenantIdAndEventoAndCanalAndAtivoTrueAndDeletedFalse(
                        envio.getTenantId(), envio.getEvento(), envio.getCanal())
                .map(t -> t.getTemplateExterno())
                .orElse(null);
    }

    private Map<String, String> variaveis(AccNotificacaoEnvio envio) {
        if (envio.getPayloadJson() == null || envio.getPayloadJson().isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(envio.getPayloadJson(), new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }

    private boolean tetoDiarioAtingido(AccNotificacaoEnvio envio, AccNotificacaoConfig config) {
        if (config == null || config.getLimiteDiario() == null || config.getLimiteDiario() <= 0) {
            return false;
        }
        Instant inicioDoDia = LocalDate.now(FUSO).atStartOfDay(FUSO).toInstant();
        long jaEnviados = envioRepository.contarEnviadosDesde(envio.getTenantId(), envio.getCanal(), inicioDoDia);
        return jaEnviados >= config.getLimiteDiario();
    }

    static Instant inicioDoProximoDia() {
        return LocalDate.now(FUSO).plusDays(1).atTime(LocalTime.of(7, 0)).atZone(FUSO).toInstant();
    }

    /**
     * Varredura a cada 60s. Existe porque {@code @Async} nao sobrevive a um
     * restart: o que ficou PENDENTE em memoria volta por aqui.
     */
    // A varredura roda em thread de scheduler, sem request e sem transacao.
    // O TenantRepositoryAspect aplica o filtro de tenant do Hibernate e
    // precisa de um EntityManager transacional: sem @Transactional aqui, a
    // fila inteira falhava a cada 60s com "No EntityManager with actual
    // transaction available".
    @Scheduled(fixedDelayString = "${app.access.notificacao.varredura-ms:60000}")
    @Transactional
    public void varrer() {
        try {
            Instant agora = Instant.now();

            // Primeiro o corte de expiracao, para nao gastar chamada de
            // provedor com aviso que ja perdeu a validade.
            int expirados = envioRepository.expirarAntigos(
                    List.of(StatusEnvio.PENDENTE, StatusEnvio.FALHOU),
                    agora.minus(JANELA_UTIL), StatusEnvio.EXPIRADO, agora);
            if (expirados > 0) {
                log.warn("{} notificacoes expiradas por passarem de {}h", expirados, JANELA_UTIL.toHours());
            }

            List<AccNotificacaoEnvio> elegiveis = envioRepository.buscarElegiveis(
                    List.of(StatusEnvio.PENDENTE, StatusEnvio.FALHOU), agora, PageRequest.of(0, LOTE));

            for (AccNotificacaoEnvio envio : elegiveis) {
                // FALHOU transitorio precisa voltar a PENDENTE: o claim atomico
                // so' aceita PENDENTE, e e' ele que impede envio duplicado.
                if (envio.getStatus() == StatusEnvio.FALHOU) {
                    filaWriter.reagendarSemPenalidade(envio.getId(), envio.getAgendadoPara(), null);
                }
                processar(envio.getId());
            }
        } catch (RuntimeException e) {
            // Varredura nunca pode morrer: se ela para, a fila para.
            log.error("Falha na varredura da fila de notificacoes: {}", e.toString(), e);
        }
    }
}
