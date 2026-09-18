package br.com.alfaschool.backend.access.painel;

import br.com.alfaschool.backend.application.access.painel.PainelAcessoService;
import br.com.alfaschool.backend.application.access.painel.PainelTokenService;
import br.com.alfaschool.backend.application.access.painel.dto.PainelEstadoResponse;
import br.com.alfaschool.backend.application.access.retirada.RetiradaConsultaService;
import br.com.alfaschool.backend.application.access.retirada.dto.RetiradaFilaItem;
import br.com.alfaschool.backend.domain.access.painel.AccPainel;
import br.com.alfaschool.backend.domain.access.painel.AccPainelDispositivo;
import br.com.alfaschool.backend.domain.access.painel.AccPainelFonte;
import br.com.alfaschool.backend.domain.access.shared.EscopoPainel;
import br.com.alfaschool.backend.domain.access.shared.StatusRetirada;
import br.com.alfaschool.backend.domain.access.shared.TipoPainel;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccPainelDispositivoRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccPainelFonteRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccPainelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PainelAcessoServiceTest {

    private static final UUID TENANT_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID TENANT_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    private static final UUID UNIDADE = UUID.fromString("11111111-0000-0000-0000-000000000001");
    private static final UUID SALA_A = UUID.fromString("5a1a0000-0000-0000-0000-00000000000a");
    private static final UUID SALA_B = UUID.fromString("5a1a0000-0000-0000-0000-00000000000b");
    private static final UUID TURMA_A = UUID.fromString("70a70000-0000-0000-0000-00000000000a");

    @Mock private AccPainelRepository painelRepository;
    @Mock private AccPainelFonteRepository fonteRepository;
    @Mock private AccPainelDispositivoRepository dispositivoRepository;
    @Mock private RetiradaConsultaService consultaService;

    private final PainelTokenService tokenService = new PainelTokenService();
    private PainelAcessoService service;

    /** Fila ficticia da escola inteira: duas salas diferentes. */
    private final RetiradaFilaItem joaoDaSalaA = item("Joao", SALA_A, TURMA_A);
    private final RetiradaFilaItem mariaDaSalaB = item("Maria", SALA_B, null);

    @BeforeEach
    void preparar() {
        service = new PainelAcessoService(painelRepository, fonteRepository, dispositivoRepository,
                tokenService, consultaService);

        // O mock responde como o SQL real responderia: devolve apenas o que
        // cai dentro do recorte pedido. Assim o teste prova o filtro, e nao
        // apenas que o metodo foi chamado.
        when(consultaService.filaDoRecorte(any(), any(), any(), any(), any(), anyBoolean()))
                .thenAnswer(inv -> {
                    Collection<UUID> turmas = inv.getArgument(2);
                    Collection<UUID> salas = inv.getArgument(3);
                    boolean unidadeInteira = inv.getArgument(5);
                    if (unidadeInteira) {
                        return List.of(joaoDaSalaA, mariaDaSalaB);
                    }
                    return List.of(joaoDaSalaA, mariaDaSalaB).stream()
                            .filter(i -> (salas != null && salas.contains(i.salaId()))
                                    || (turmas != null && i.turmaId() != null && turmas.contains(i.turmaId())))
                            .toList();
                });
    }

    private RetiradaFilaItem item(String nome, UUID salaId, UUID turmaId) {
        return new RetiradaFilaItem(UUID.randomUUID(), UNIDADE,
                new RetiradaFilaItem.AlunoDoCartao(UUID.randomUUID(), nome, "faces/" + nome, null, "Turma", "Sala"),
                new RetiradaFilaItem.RetiranteDoCartao(UUID.randomUUID(), "Responsavel", "faces/resp", null, "mae"),
                turmaId, "Turma", salaId, "Sala", null, null,
                StatusRetirada.SOLICITADA, 1, Instant.now(), null, null, null,
                null, false, null, null, 3);
    }

    private AccPainel painel(UUID tenantId, String slug, TipoPainel tipo) {
        AccPainel painel = new AccPainel();
        painel.setTenantId(tenantId);
        painel.setUnitId(UNIDADE);
        painel.setSlug(slug);
        painel.setNome("Painel " + slug);
        painel.setTipo(tipo);
        painel.setAtivo(true);
        ReflectionTestUtils.setField(painel, "id", UUID.randomUUID(), null);
        return painel;
    }

    private AccPainelDispositivo dispositivo(AccPainel painel, String tokenEmClaro, boolean revogado) {
        AccPainelDispositivo d = new AccPainelDispositivo();
        d.setTenantId(painel.getTenantId());
        d.setPainelId(painel.getId());
        d.setNome("TV");
        d.setTokenHash(tokenService.hash(tokenEmClaro));
        d.setTokenPrefixo(tokenEmClaro.substring(0, 8));
        d.setRevogado(revogado);
        ReflectionTestUtils.setField(d, "id", UUID.randomUUID(), null);
        return d;
    }

    private AccPainelFonte fonte(AccPainel painel, EscopoPainel escopo, UUID referencia) {
        AccPainelFonte f = new AccPainelFonte();
        f.setTenantId(painel.getTenantId());
        f.setPainelId(painel.getId());
        f.setEscopo(escopo);
        f.setReferenciaId(referencia);
        return f;
    }

    private void registrar(AccPainel painel, AccPainelDispositivo dispositivo, String token) {
        when(dispositivoRepository.findByTokenHashAndDeletedFalse(tokenService.hash(token)))
                .thenReturn(Optional.of(dispositivo));
        when(painelRepository.findByIdAndTenantIdAndDeletedFalse(painel.getId(), painel.getTenantId()))
                .thenReturn(Optional.of(painel));
    }

    // =================================================================
    // Token
    // =================================================================

    @Test
    @DisplayName("Token valido autentica e carimba o ultimo acesso")
    void tokenValido() {
        AccPainel painel = painel(TENANT_A, "sala-1a", TipoPainel.SALA);
        AccPainelDispositivo tv = dispositivo(painel, "token-valido-1234567890", false);
        registrar(painel, tv, "token-valido-1234567890");

        PainelAcessoService.PainelAutenticado autenticado =
                service.autenticar("sala-1a", "token-valido-1234567890", "10.0.0.5", "SmartTV/1.0");

        assertEquals(painel.getId(), autenticado.painel().getId());
        assertEquals("painel:" + painel.getId(), autenticado.topico());
        assertNotNull(tv.getUltimoAcesso());
        assertEquals("10.0.0.5", tv.getUltimoIp());
        assertEquals("SmartTV/1.0", tv.getUserAgent());
        verify(dispositivoRepository).save(tv);
    }

    @Test
    @DisplayName("Token invalido: 401")
    void tokenInvalido() {
        when(dispositivoRepository.findByTokenHashAndDeletedFalse(any())).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.autenticar("sala-1a", "token-que-nao-existe", "10.0.0.5", null));
        assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatusCode());
    }

    @Test
    @DisplayName("Sem token nenhum: 401 — a URL permanente nao autentica")
    void semToken() {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.autenticar("sala-1a", null, "10.0.0.5", null));
        assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatusCode());

        ResponseStatusException vazio = assertThrows(ResponseStatusException.class,
                () -> service.autenticar("sala-1a", "   ", "10.0.0.5", null));
        assertEquals(HttpStatus.UNAUTHORIZED, vazio.getStatusCode());
    }

    @Test
    @DisplayName("Token revogado: 403 imediato")
    void tokenRevogado() {
        AccPainel painel = painel(TENANT_A, "sala-1a", TipoPainel.SALA);
        AccPainelDispositivo tv = dispositivo(painel, "token-revogado-123456", true);
        registrar(painel, tv, "token-revogado-123456");

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.autenticar("sala-1a", "token-revogado-123456", "10.0.0.5", null));
        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
    }

    @Test
    @DisplayName("Token de um painel nao abre a tela de outro")
    void tokenDeOutroPainelNaoServe() {
        AccPainel portaria = painel(TENANT_A, "portaria", TipoPainel.PORTARIA);
        AccPainelDispositivo tv = dispositivo(portaria, "token-portaria-000000", false);
        registrar(portaria, tv, "token-portaria-000000");

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.autenticar("sala-1a", "token-portaria-000000", "10.0.0.5", null));
        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
    }

    @Test
    @DisplayName("Painel inativo nao transmite")
    void painelInativo() {
        AccPainel painel = painel(TENANT_A, "sala-1a", TipoPainel.SALA);
        painel.setAtivo(false);
        AccPainelDispositivo tv = dispositivo(painel, "token-de-painel-inativo", false);
        registrar(painel, tv, "token-de-painel-inativo");

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.autenticar("sala-1a", "token-de-painel-inativo", "10.0.0.5", null));
        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
    }

    @Test
    @DisplayName("Painel de outro tenant nao e alcancado pelo token")
    void tenantVemDoTokenNaoDaUrl() {
        AccPainel painelDeOutroTenant = painel(TENANT_B, "sala-1a", TipoPainel.SALA);
        AccPainelDispositivo tv = dispositivo(painelDeOutroTenant, "token-tenant-b-00000", false);

        when(dispositivoRepository.findByTokenHashAndDeletedFalse(tokenService.hash("token-tenant-b-00000")))
                .thenReturn(Optional.of(tv));
        // O painel e procurado com o tenant do DISPOSITIVO. Nenhum painel do
        // tenant A e alcancavel com um token do tenant B.
        when(painelRepository.findByIdAndTenantIdAndDeletedFalse(painelDeOutroTenant.getId(), TENANT_B))
                .thenReturn(Optional.of(painelDeOutroTenant));

        PainelAcessoService.PainelAutenticado autenticado =
                service.autenticar("sala-1a", "token-tenant-b-00000", "10.0.0.5", null);

        assertEquals(TENANT_B, autenticado.tenantId());
        verify(painelRepository).findByIdAndTenantIdAndDeletedFalse(painelDeOutroTenant.getId(), TENANT_B);
    }

    // =================================================================
    // Isolamento do recorte
    // =================================================================

    @Test
    @DisplayName("ISOLAMENTO: painel da sala A nao lista aluno da sala B")
    void painelDaSalaANaoVeAlunoDaSalaB() {
        AccPainel painelSalaA = painel(TENANT_A, "sala-a", TipoPainel.SALA);
        when(fonteRepository.findByTenantIdAndPainelId(TENANT_A, painelSalaA.getId()))
                .thenReturn(List.of(fonte(painelSalaA, EscopoPainel.SALA, SALA_A)));

        List<RetiradaFilaItem> itens = service.itensDoRecorte(painelSalaA);

        assertEquals(1, itens.size());
        assertEquals("Joao", itens.get(0).aluno().nome());
        assertTrue(itens.stream().noneMatch(i -> SALA_B.equals(i.salaId())),
                "A tela da sala A jamais pode listar crianca da sala B");

        // E o filtro foi de fato empurrado para a consulta, com a sala A e
        // so' ela.
        verify(consultaService).filaDoRecorte(eq(TENANT_A), eq(UNIDADE), eq(List.of()),
                eq(List.of(SALA_A)), eq(List.of()), eq(false));
    }

    @Test
    @DisplayName("Fonte por TURMA filtra por turma, nao pela escola")
    void fonteDeTurmaFiltraPorTurma() {
        AccPainel painelTurma = painel(TENANT_A, "turma-a", TipoPainel.SALA);
        when(fonteRepository.findByTenantIdAndPainelId(TENANT_A, painelTurma.getId()))
                .thenReturn(List.of(fonte(painelTurma, EscopoPainel.TURMA, TURMA_A)));

        List<RetiradaFilaItem> itens = service.itensDoRecorte(painelTurma);

        assertEquals(1, itens.size());
        assertEquals("Joao", itens.get(0).aluno().nome());
        verify(consultaService).filaDoRecorte(eq(TENANT_A), eq(UNIDADE), eq(List.of(TURMA_A)),
                eq(List.of()), eq(List.of()), eq(false));
    }

    @Test
    @DisplayName("Painel SEM fonte nao vira painel de tudo")
    void painelSemFonteNaoVeNada() {
        AccPainel painelSemFonte = painel(TENANT_A, "orfao", TipoPainel.SALA);
        when(fonteRepository.findByTenantIdAndPainelId(TENANT_A, painelSemFonte.getId()))
                .thenReturn(List.of());

        service.itensDoRecorte(painelSemFonte);

        // Nao pode pedir a escola inteira: falha fechada.
        verify(consultaService).filaDoRecorte(eq(TENANT_A), eq(UNIDADE), eq(List.of()),
                eq(List.of()), eq(List.of()), eq(false));
    }

    @Test
    @DisplayName("Estado devolve o recorte junto com a politica de exibicao da tela")
    void estadoCarregaRecorteEPolitica() {
        AccPainel painelSalaA = painel(TENANT_A, "sala-a", TipoPainel.SALA);
        painelSalaA.setExibeFoto(false);
        painelSalaA.setRetencaoSeg(15);
        AccPainelDispositivo tv = dispositivo(painelSalaA, "token-estado-0000000", false);
        registrar(painelSalaA, tv, "token-estado-0000000");
        when(fonteRepository.findByTenantIdAndPainelId(TENANT_A, painelSalaA.getId()))
                .thenReturn(List.of(fonte(painelSalaA, EscopoPainel.SALA, SALA_A)));

        PainelAcessoService.PainelAutenticado autenticado =
                service.autenticar("sala-a", "token-estado-0000000", "10.0.0.5", null);
        PainelEstadoResponse estado = service.estado(autenticado);

        assertEquals(1, estado.retiradas().size());
        assertEquals("Joao", estado.retiradas().get(0).aluno().nome());
        assertEquals(false, estado.painel().exibeFoto());
        assertEquals(15, estado.painel().retencaoSeg());
        // Chave de foto, nunca bytes.
        assertTrue(estado.retiradas().get(0).aluno().fotoKey().startsWith("faces/"));
    }
}
