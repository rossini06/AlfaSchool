package br.com.alfaschool.backend.application.access.painel;

import br.com.alfaschool.backend.application.access.painel.dto.PainelEstadoResponse;
import br.com.alfaschool.backend.application.access.retirada.RetiradaConsultaService;
import br.com.alfaschool.backend.application.access.retirada.dto.RetiradaFilaItem;
import br.com.alfaschool.backend.domain.access.painel.AccPainel;
import br.com.alfaschool.backend.domain.access.painel.AccPainelDispositivo;
import br.com.alfaschool.backend.domain.access.painel.AccPainelFonte;
import br.com.alfaschool.backend.domain.access.shared.EscopoPainel;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccPainelDispositivoRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccPainelFonteRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccPainelRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de entrada das TVs.
 *
 * REQUISITO EXPLICITO DO CLIENTE: a URL permanente identifica o painel, mas
 * NAO autentica. Sem dispositivo autorizado nao ha' acesso. Quem autentica
 * e' o token; o slug so' diz qual tela se espera ver, e ainda e' conferido
 * contra o painel do token para que um token de portaria nao abra a tela da
 * sala do 1o ano.
 */
@Service
public class PainelAcessoService {

    /** Painel autenticado por token de dispositivo. */
    public record PainelAutenticado(AccPainel painel, AccPainelDispositivo dispositivo) {

        public UUID tenantId() {
            return painel.getTenantId();
        }

        public String topico() {
            return "painel:" + painel.getId();
        }
    }

    private final AccPainelRepository painelRepository;
    private final AccPainelFonteRepository fonteRepository;
    private final AccPainelDispositivoRepository dispositivoRepository;
    private final PainelTokenService tokenService;
    private final RetiradaConsultaService consultaService;

    public PainelAcessoService(AccPainelRepository painelRepository,
                               AccPainelFonteRepository fonteRepository,
                               AccPainelDispositivoRepository dispositivoRepository,
                               PainelTokenService tokenService,
                               RetiradaConsultaService consultaService) {
        this.painelRepository = painelRepository;
        this.fonteRepository = fonteRepository;
        this.dispositivoRepository = dispositivoRepository;
        this.tokenService = tokenService;
        this.consultaService = consultaService;
    }

    /**
     * Valida o token e devolve o painel correspondente.
     *
     * A busca e' feita pelo HASH, nunca pelo slug: e' o token que determina
     * o tenant e o painel. Fazer o contrario permitiria escolher a tela e
     * so' depois provar o direito a ela.
     */
    @Transactional
    public PainelAutenticado autenticar(String slug, String token, String ip, String userAgent) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Token do painel ausente: informe X-Painel-Token");
        }
        Optional<AccPainelDispositivo> encontrado =
                dispositivoRepository.findByTokenHashAndDeletedFalse(tokenService.hash(token));
        if (encontrado.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token do painel invalido");
        }
        AccPainelDispositivo dispositivo = encontrado.get();

        // Confirmacao em tempo constante: o indice ja' achou a linha, mas a
        // comparacao final nao pode vazar nada pelo tempo de resposta.
        if (!tokenService.confere(token, dispositivo.getTokenHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token do painel invalido");
        }
        if (dispositivo.isRevogado()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Dispositivo do painel revogado");
        }

        AccPainel painel = painelRepository
                .findByIdAndTenantIdAndDeletedFalse(dispositivo.getPainelId(), dispositivo.getTenantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Painel nao encontrado"));
        if (!painel.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Painel inativo");
        }
        // O slug da URL tem de ser o do painel do token. Sem isso, uma TV da
        // portaria poderia abrir a URL da sala e receber o fluxo dela.
        if (slug != null && !slug.equals(painel.getSlug())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Token nao pertence a este painel");
        }

        dispositivo.setUltimoAcesso(Instant.now());
        dispositivo.setUltimoIp(ip);
        dispositivo.setUserAgent(recortar(userAgent, 255));
        dispositivoRepository.save(dispositivo);

        return new PainelAutenticado(painel, dispositivo);
    }

    /**
     * Estado completo do painel. So' o recorte das fontes.
     *
     * Um painel de sala devolve APENAS os alunos daquela sala naquele
     * momento; jamais a escola inteira. Painel sem fonte devolve lista
     * vazia, nao tudo.
     */
    public PainelEstadoResponse estado(PainelAutenticado autenticado) {
        AccPainel painel = autenticado.painel();
        return new PainelEstadoResponse(
                PainelEstadoResponse.PainelDaTela.from(painel),
                Instant.now(),
                itensParaExibicao(painel));
    }

    /**
     * Os itens do recorte, ja' com a politica de foto aplicada.
     *
     * "Nao exibir foto" precisa acontecer AQUI. Antes, o painel com
     * exibeFoto=false recebia a URL assinada da foto assim mesmo e apenas a
     * tela deixava de desenhar a imagem — quem tivesse o token da TV, ou
     * abrisse a aba de rede do navegador, baixava a foto da crianca.
     * Politica de exibicao decidida no cliente nao e' politica.
     *
     * Vale para o estado inicial e para tudo que vai pelo SSE: os dois
     * caminhos passam por este metodo.
     */
    public List<RetiradaFilaItem> itensParaExibicao(AccPainel painel) {
        List<RetiradaFilaItem> itens = itensDoRecorte(painel);
        if (painel.isExibeFoto()) {
            return itens;
        }
        return itens.stream().map(RetiradaFilaItem::semFotos).toList();
    }

    public List<RetiradaFilaItem> itensDoRecorte(AccPainel painel) {
        List<AccPainelFonte> fontes = fonteRepository.findByTenantIdAndPainelId(painel.getTenantId(), painel.getId());
        List<UUID> turmas = new ArrayList<>();
        List<UUID> salas = new ArrayList<>();
        List<UUID> portarias = new ArrayList<>();
        boolean unidadeInteira = false;
        for (AccPainelFonte fonte : fontes) {
            if (fonte.getEscopo() == EscopoPainel.TURMA && fonte.getReferenciaId() != null) {
                turmas.add(fonte.getReferenciaId());
            } else if (fonte.getEscopo() == EscopoPainel.SALA && fonte.getReferenciaId() != null) {
                salas.add(fonte.getReferenciaId());
            } else if (fonte.getEscopo() == EscopoPainel.PORTARIA && fonte.getReferenciaId() != null) {
                portarias.add(fonte.getReferenciaId());
            } else if (fonte.getEscopo() == EscopoPainel.UNIDADE) {
                unidadeInteira = true;
            }
        }
        return consultaService.filaDoRecorte(
                painel.getTenantId(), painel.getUnitId(), turmas, salas, portarias, unidadeInteira);
    }

    private static String recortar(String valor, int tamanho) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= tamanho ? valor : valor.substring(0, tamanho);
    }
}
