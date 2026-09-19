package br.com.alfaschool.backend.application.access.autorizacao;

import br.com.alfaschool.backend.application.access.autorizacao.dto.AlunoAutorizadoResponse;
import br.com.alfaschool.backend.application.access.autorizacao.dto.PessoaAutorizadaResponse;
import br.com.alfaschool.backend.application.access.shared.AutorizacaoPort;
import br.com.alfaschool.backend.application.access.shared.AutorizacaoPort.MotivoNegativa;
import br.com.alfaschool.backend.domain.access.autorizacao.AutorizacaoRetirada;
import br.com.alfaschool.backend.domain.access.autorizacao.PessoaAutorizada;
import br.com.alfaschool.backend.domain.access.autorizacao.Restricao;
import br.com.alfaschool.backend.domain.access.shared.StatusAutorizacao;
import br.com.alfaschool.backend.domain.aluno.Aluno;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccAutorizacaoRetiradaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccPessoaAutorizadaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccRestricaoRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AlunoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Decide se uma pessoa pode retirar um aluno AGORA.
 *
 * Este e' o ponto do sistema em que um erro entrega uma crianca a pessoa
 * errada. Duas leis governam o arquivo inteiro:
 *
 * FALHA FECHADA — qualquer duvida nega. Excecao inesperada e' logada em
 * ERROR e vira negacao; nunca ha caminho que devolva permitido por omissao.
 *
 * FUSO EXPLICITO — o Instant recebido e' UTC. Avaliar dia da semana e hora
 * em UTC libera a pessoa no dia errado: 2024-03-05T02:00Z ainda e' dia 4 em
 * Sao Paulo. Tudo e' convertido para America/Sao_Paulo antes de comparar.
 */
@Service
public class AutorizacaoConsultaService implements AutorizacaoPort {

    private static final Logger log = LoggerFactory.getLogger(AutorizacaoConsultaService.class);

    /**
     * Fuso da operacao escolar. Fixo de proposito: o Instant e' UTC e o
     * horario da escola nao muda com o servidor onde a JVM roda.
     */
    static final ZoneId FUSO_ESCOLA = ZoneId.of("America/Sao_Paulo");

    private static final String[] NOMES_DIAS = {
            "segunda", "terca", "quarta", "quinta", "sexta", "sabado", "domingo"
    };

    private final AccRestricaoRepository restricaoRepository;
    private final AccPessoaAutorizadaRepository pessoaRepository;
    private final AccAutorizacaoRetiradaRepository autorizacaoRepository;
    private final AlunoRepository alunoRepository;

    public AutorizacaoConsultaService(AccRestricaoRepository restricaoRepository,
                                      AccPessoaAutorizadaRepository pessoaRepository,
                                      AccAutorizacaoRetiradaRepository autorizacaoRepository,
                                      AlunoRepository alunoRepository) {
        this.restricaoRepository = restricaoRepository;
        this.pessoaRepository = pessoaRepository;
        this.autorizacaoRepository = autorizacaoRepository;
        this.alunoRepository = alunoRepository;
    }

    /**
     * ORDEM DE AVALIACAO (nao reordene):
     *
     * 1. Restricao ativa e vigente para (aluno, pessoa), casando por id OU
     *    por CPF solto. PRIMEIRO DE TUDO — o pai afastado por medida
     *    protetiva costuma estar cadastrado e autorizado de antes da decisao,
     *    entao checar autorizacao antes da restricao inverteria a precedencia.
     * 2. Pessoa existe, nao deletada, ativa e podeRetirar = true.
     * 3. Existe autorizacao (aluno, pessoa) ATIVA — com motivo especifico
     *    quando o que existe esta PENDENTE, SUSPENSA, REVOGADA ou EXPIRADA.
     * 4. Vigencia cobre a data do momento.
     * 5. Dias da semana, se preenchidos, contem o dia do momento.
     * 6. Faixa de horario, se preenchida, contem a hora do momento.
     * 7. Nada barrou: permite, devolvendo qual autorizacao valeu.
     */
    @Override
    @Transactional(readOnly = true)
    public Veredito verificar(UUID alunoId, UUID pessoaAutorizadaId, Instant momento) {
        try {
            if (alunoId == null || pessoaAutorizadaId == null || momento == null) {
                return Veredito.negar(MotivoNegativa.DADOS_INSUFICIENTES, "Dados insuficientes para validar a retirada");
            }

            UUID tenantId = ContextoAtual.tenantObrigatorio();
            ZonedDateTime local = momento.atZone(FUSO_ESCOLA);
            LocalDate data = local.toLocalDate();
            LocalTime hora = local.toLocalTime();

            // A pessoa e' carregada antes do passo 1 apenas para obter o CPF,
            // necessario ao casamento da restricao solta. A DECISAO sobre ela
            // (passo 2) continua vindo depois da restricao.
            Optional<PessoaAutorizada> pessoaOpt =
                    pessoaRepository.findByIdAndTenantIdAndDeletedFalse(pessoaAutorizadaId, tenantId);
            String cpfPessoa = pessoaOpt.map(PessoaAutorizada::getCpf).map(CpfUtils::normalizar).orElse(null);

            // 1. RESTRICAO — precedencia absoluta sobre qualquer autorizacao,
            // inclusive ATIVA e permanente.
            List<Restricao> restricoes =
                    restricaoRepository.findByTenantIdAndAlunoIdAndAtivoTrueAndDeletedFalse(tenantId, alunoId);
            for (Restricao restricao : restricoes) {
                if (restricaoAlcanca(restricao, pessoaAutorizadaId, cpfPessoa) && restricao.vigenteEm(data)) {
                    return Veredito.negar(MotivoNegativa.RESTRICAO_JUDICIAL, "Restricao judicial vigente");
                }
            }

            // 2. PESSOA — cadastro valido e a permissao podeRetirar, que e'
            // independente de portal e de notificacao.
            PessoaAutorizada pessoa = pessoaOpt.orElse(null);
            if (pessoa == null) {
                return Veredito.negar(MotivoNegativa.PESSOA_NAO_ENCONTRADA, "Pessoa autorizada nao encontrada");
            }
            if (!pessoa.isAtivo()) {
                return Veredito.negar(MotivoNegativa.PESSOA_INATIVA, "Pessoa autorizada inativa");
            }
            if (!pessoa.isPodeRetirar()) {
                return Veredito.negar(MotivoNegativa.SEM_PERMISSAO_RETIRADA, "Pessoa sem permissao de retirada");
            }

            // 3. AUTORIZACAO — precisa existir uma ATIVA para este par.
            List<AutorizacaoRetirada> autorizacoes = autorizacaoRepository
                    .findByTenantIdAndAlunoIdAndPessoaAutorizadaIdAndDeletedFalse(tenantId, alunoId, pessoaAutorizadaId);
            if (autorizacoes.isEmpty()) {
                return Veredito.negar(MotivoNegativa.SEM_AUTORIZACAO, "Pessoa nao autorizada a retirar este aluno");
            }

            List<AutorizacaoRetirada> ativas = autorizacoes.stream()
                    .filter(a -> a.getStatus() == StatusAutorizacao.ATIVA)
                    .toList();
            if (ativas.isEmpty()) {
                return Veredito.negar(MotivoNegativa.AUTORIZACAO_NAO_ATIVA, motivoDeStatusNaoAtivo(autorizacoes));
            }

            // Pode haver mais de uma ATIVA (ex.: permanente restrita a certos
            // dias + temporaria de um dia). Basta UMA cobrir o momento; a
            // negativa reportada e' a da autorizacao que chegou mais perto.
            String motivoMaisProximo = null;
            for (AutorizacaoRetirada autorizacao : ativas) {
                // 4. VIGENCIA
                if (autorizacao.getVigenciaInicio() != null && data.isBefore(autorizacao.getVigenciaInicio())) {
                    motivoMaisProximo = manterMotivo(motivoMaisProximo, "Autorizacao ainda nao vigente");
                    continue;
                }
                if (autorizacao.getVigenciaFim() != null && data.isAfter(autorizacao.getVigenciaFim())) {
                    motivoMaisProximo = manterMotivo(motivoMaisProximo, "Autorizacao fora do periodo de vigencia");
                    continue;
                }

                // 5. DIA DA SEMANA (ISO: 1=segunda ... 7=domingo)
                if (!diaPermitido(autorizacao.getDiasSemana(), data.getDayOfWeek())) {
                    motivoMaisProximo = manterMotivo(motivoMaisProximo,
                            "Autorizada apenas em " + descreverDias(autorizacao.getDiasSemana()));
                    continue;
                }

                // 6. FAIXA DE HORARIO — extremos INCLUSIVOS: quem chega
                // exatamente na hora de inicio ou de fim esta dentro.
                if (!horaPermitida(autorizacao.getHoraInicio(), autorizacao.getHoraFim(), hora)) {
                    motivoMaisProximo = manterMotivo(motivoMaisProximo,
                            "Autorizada apenas entre " + autorizacao.getHoraInicio()
                                    + " e " + autorizacao.getHoraFim());
                    continue;
                }

                // 7. Passou por tudo.
                return Veredito.permitir(autorizacao.getId());
            }

            return Veredito.negar(MotivoNegativa.FORA_DA_JANELA, motivoMaisProximo != null ? motivoMaisProximo : "Autorizacao nao vigente");

        } catch (Exception e) {
            // FALHA FECHADA: erro nunca vira permissao. Logamos em ERROR para
            // que a falha apareca, e devolvemos negado.
            log.error("Falha ao validar autorizacao de retirada (aluno={}, pessoa={}, momento={})",
                    alunoId, pessoaAutorizadaId, momento, e);
            return Veredito.negar(MotivoNegativa.ERRO_INTERNO, "Falha ao validar autorizacao");
        }
    }

    /**
     * Quem pode retirar este aluno neste momento. Cada candidato passa pela
     * verificacao completa — nao ha atalho que pule restricao ou horario.
     */
    @Transactional(readOnly = true)
    public List<PessoaAutorizadaResponse> quemPodeRetirarAgora(UUID alunoId, Instant momento) {
        UUID tenantId = ContextoAtual.tenantObrigatorio();
        if (alunoId == null || momento == null) {
            return List.of();
        }

        List<UUID> candidatos = autorizacaoRepository
                .findByTenantIdAndAlunoIdAndStatusAndDeletedFalse(tenantId, alunoId, StatusAutorizacao.ATIVA)
                .stream()
                .map(AutorizacaoRetirada::getPessoaAutorizadaId)
                .distinct()
                .toList();
        if (candidatos.isEmpty()) {
            return List.of();
        }

        List<PessoaAutorizadaResponse> liberadas = new ArrayList<>();
        for (PessoaAutorizada pessoa : pessoaRepository
                .findByTenantIdAndIdInAndDeletedFalse(tenantId, candidatos)) {
            if (verificar(alunoId, pessoa.getId(), momento).permitido()) {
                liberadas.add(PessoaAutorizadaResponse.from(pessoa));
            }
        }
        liberadas.sort(Comparator.comparing(PessoaAutorizadaResponse::nome,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return liberadas;
    }

    /**
     * O inverso, usado pelo leitor da portaria: identificada a pessoa,
     * quais alunos ela pode retirar neste momento.
     */
    @Transactional(readOnly = true)
    public List<AlunoAutorizadoResponse> alunosQuePodeRetirar(UUID pessoaAutorizadaId, Instant momento) {
        UUID tenantId = ContextoAtual.tenantObrigatorio();
        if (pessoaAutorizadaId == null || momento == null) {
            return List.of();
        }

        List<AlunoAutorizadoResponse> liberados = new ArrayList<>();
        for (AutorizacaoRetirada autorizacao : autorizacaoRepository
                .findByTenantIdAndPessoaAutorizadaIdAndStatusAndDeletedFalse(
                        tenantId, pessoaAutorizadaId, StatusAutorizacao.ATIVA)) {

            Veredito veredito = verificar(autorizacao.getAlunoId(), pessoaAutorizadaId, momento);
            if (!veredito.permitido()) {
                continue;
            }
            Aluno aluno = alunoRepository.findById(autorizacao.getAlunoId())
                    .filter(a -> tenantId.equals(a.getTenantId()) && !Boolean.TRUE.equals(a.getDeleted()))
                    .orElse(null);
            if (aluno == null) {
                continue;
            }
            liberados.add(new AlunoAutorizadoResponse(
                    aluno.getId(), aluno.getNome(), aluno.getFoto(), veredito.autorizacaoId()));
        }
        liberados.sort(Comparator.comparing(AlunoAutorizadoResponse::alunoNome,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return liberados;
    }

    // ------------------------------------------------------------------
    // Regras isoladas
    // ------------------------------------------------------------------

    /**
     * A restricao alcanca a pessoa por id OU por CPF. O CPF e' o que bloqueia
     * quem ainda nao estava cadastrado quando a decisao chegou — e quem se
     * recadastrar depois com outro registro.
     */
    private boolean restricaoAlcanca(Restricao restricao, UUID pessoaAutorizadaId, String cpfPessoa) {
        if (restricao.getPessoaAutorizadaId() != null
                && restricao.getPessoaAutorizadaId().equals(pessoaAutorizadaId)) {
            return true;
        }
        String cpfRestricao = CpfUtils.normalizar(restricao.getPessoaCpf());
        return cpfRestricao != null && cpfRestricao.equals(cpfPessoa);
    }

    /**
     * Motivo especifico por status. A portaria precisa saber a diferenca
     * entre "aguardando aprovacao" e "suspensa": a primeira a coordenacao
     * resolve na hora, a segunda foi uma decisao deliberada.
     */
    private String motivoDeStatusNaoAtivo(List<AutorizacaoRetirada> autorizacoes) {
        boolean pendente = autorizacoes.stream().anyMatch(a -> a.getStatus() == StatusAutorizacao.PENDENTE);
        if (pendente) {
            return "Autorizacao aguardando aprovacao da escola";
        }
        boolean suspensa = autorizacoes.stream().anyMatch(a -> a.getStatus() == StatusAutorizacao.SUSPENSA);
        if (suspensa) {
            return "Autorizacao suspensa";
        }
        return "Autorizacao nao vigente";
    }

    /** CSV vazio/nulo significa "todos os dias". */
    private boolean diaPermitido(String diasSemana, DayOfWeek dia) {
        if (diasSemana == null || diasSemana.isBlank()) {
            return true;
        }
        String alvo = String.valueOf(dia.getValue());
        for (String parte : diasSemana.split(",")) {
            if (parte.trim().equals(alvo)) {
                return true;
            }
        }
        return false;
    }

    /** Faixa aberta de um dos lados continua valendo do outro. */
    private boolean horaPermitida(LocalTime inicio, LocalTime fim, LocalTime hora) {
        if (inicio != null && hora.isBefore(inicio)) {
            return false;
        }
        return fim == null || !hora.isAfter(fim);
    }

    private String descreverDias(String diasSemana) {
        if (diasSemana == null || diasSemana.isBlank()) {
            return "todos os dias";
        }
        List<String> nomes = new ArrayList<>();
        for (String parte : diasSemana.split(",")) {
            String token = parte.trim();
            if (token.isEmpty()) {
                continue;
            }
            try {
                int dia = Integer.parseInt(token);
                if (dia >= 1 && dia <= 7) {
                    nomes.add(NOMES_DIAS[dia - 1]);
                }
            } catch (NumberFormatException ignored) {
                // CSV invalido ja foi barrado na escrita; aqui so nao descreve.
            }
        }
        return nomes.isEmpty() ? diasSemana : String.join(", ", nomes);
    }

    /** Mantem o primeiro motivo encontrado, para a mensagem nao ficar vaga. */
    private String manterMotivo(String atual, String novo) {
        return atual != null ? atual : novo;
    }
}
