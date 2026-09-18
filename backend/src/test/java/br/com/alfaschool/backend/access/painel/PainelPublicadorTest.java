package br.com.alfaschool.backend.access.painel;

import br.com.alfaschool.backend.application.access.painel.PainelPublicador;
import br.com.alfaschool.backend.application.access.retirada.RetiradaConsultaService;
import br.com.alfaschool.backend.application.access.retirada.RetiradaStatusMudouEvent;
import br.com.alfaschool.backend.application.access.retirada.dto.RetiradaFilaItem;
import br.com.alfaschool.backend.application.access.shared.SseHub;
import br.com.alfaschool.backend.domain.access.painel.AccPainel;
import br.com.alfaschool.backend.domain.access.shared.StatusRetirada;
import br.com.alfaschool.backend.domain.access.shared.TipoPainel;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccPainelFonteRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccPainelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PainelPublicadorTest {

    private static final UUID TENANT_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID TENANT_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    private static final UUID UNIDADE = UUID.fromString("11111111-0000-0000-0000-000000000001");
    private static final UUID TURMA = UUID.fromString("70a70000-0000-0000-0000-00000000000a");
    private static final UUID SALA = UUID.fromString("5a1a0000-0000-0000-0000-00000000000a");
    private static final UUID PORTARIA = UUID.fromString("90a70000-0000-0000-0000-00000000000a");

    @Mock private AccPainelFonteRepository fonteRepository;
    @Mock private AccPainelRepository painelRepository;
    @Mock private RetiradaConsultaService consultaService;
    @Mock private SseHub sseHub;

    private PainelPublicador publicador;

    @BeforeEach
    void preparar() {
        publicador = new PainelPublicador(fonteRepository, painelRepository, consultaService, sseHub);
    }

    private RetiradaStatusMudouEvent evento(UUID tenantId, StatusRetirada anterior, StatusRetirada novo) {
        return new RetiradaStatusMudouEvent(tenantId, UUID.randomUUID(), UNIDADE, UUID.randomUUID(),
                TURMA, SALA, PORTARIA, anterior, novo);
    }

    private RetiradaFilaItem cartao() {
        return new RetiradaFilaItem(UUID.randomUUID(), UNIDADE,
                new RetiradaFilaItem.AlunoDoCartao(UUID.randomUUID(), "Joao", "faces/joao.jpg",
                        "1o Ano A", "Sala 12"),
                new RetiradaFilaItem.RetiranteDoCartao(UUID.randomUUID(), "Ana", "faces/ana.jpg", "mae"),
                TURMA, "1o Ano A", SALA, "Sala 12", PORTARIA, "Portaria Principal",
                StatusRetirada.PRONTO, 3,
                Instant.now(), null, null, null, null, false, null, null, 7);
    }

    private AccPainel painelCoordenacao(UUID tenantId) {
        AccPainel painel = new AccPainel();
        painel.setTenantId(tenantId);
        painel.setUnitId(UNIDADE);
        painel.setSlug("coordenacao");
        painel.setNome("Coordenacao");
        painel.setTipo(TipoPainel.COORDENACAO);
        ReflectionTestUtils.setField(painel, "id", UUID.randomUUID(), null);
        return painel;
    }

    @Test
    @DisplayName("Publica nos paineis que cobrem o aluno e na coordenacao da unidade")
    void publicaNosPaineisDoRecorteENaCoordenacao() {
        UUID painelDaSala = UUID.randomUUID();
        AccPainel coordenacao = painelCoordenacao(TENANT_A);

        when(fonteRepository.paineisQueCobrem(TENANT_A, UNIDADE, TURMA, SALA, PORTARIA))
                .thenReturn(List.of(painelDaSala));
        when(painelRepository.findByTenantIdAndUnitIdAndTipoAndAtivoTrueAndDeletedFalse(
                TENANT_A, UNIDADE, TipoPainel.COORDENACAO)).thenReturn(List.of(coordenacao));
        when(consultaService.porId(eq(TENANT_A), any())).thenReturn(cartao());

        publicador.publicar(evento(TENANT_A, StatusRetirada.PREPARANDO, StatusRetirada.PRONTO));

        verify(sseHub).publicar(eq(TENANT_A), eq("painel:" + painelDaSala), eq("retirada.pronto"), any());
        verify(sseHub).publicar(eq(TENANT_A), eq("painel:" + coordenacao.getId()), eq("retirada.pronto"), any());
    }

    @Test
    @DisplayName("Painel de outro tenant nao recebe nada")
    void outroTenantNaoRecebe() {
        UUID painelDoTenantA = UUID.randomUUID();
        when(fonteRepository.paineisQueCobrem(TENANT_A, UNIDADE, TURMA, SALA, PORTARIA))
                .thenReturn(List.of(painelDoTenantA));
        when(painelRepository.findByTenantIdAndUnitIdAndTipoAndAtivoTrueAndDeletedFalse(
                TENANT_A, UNIDADE, TipoPainel.COORDENACAO)).thenReturn(List.of());
        when(consultaService.porId(eq(TENANT_A), any())).thenReturn(cartao());

        publicador.publicar(evento(TENANT_A, StatusRetirada.SOLICITADA, StatusRetirada.PREPARANDO));

        ArgumentCaptor<UUID> tenantCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(sseHub).publicar(tenantCaptor.capture(), anyString(), anyString(), any());
        assertEquals(TENANT_A, tenantCaptor.getValue());

        // Nenhuma busca nem publicacao no tenant B.
        verify(sseHub, never()).publicar(eq(TENANT_B), anyString(), anyString(), any());
        verify(fonteRepository, never()).paineisQueCobrem(eq(TENANT_B), any(), any(), any(), any());
        verify(painelRepository, never()).findByTenantIdAndUnitIdAndTipoAndAtivoTrueAndDeletedFalse(
                eq(TENANT_B), any(), any());
    }

    @Test
    @DisplayName("Sem painel no recorte, nada e publicado e o cartao nem e lido")
    void semDestinoNaoPublica() {
        when(fonteRepository.paineisQueCobrem(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(painelRepository.findByTenantIdAndUnitIdAndTipoAndAtivoTrueAndDeletedFalse(any(), any(), any()))
                .thenReturn(List.of());

        publicador.publicar(evento(TENANT_A, StatusRetirada.SOLICITADA, StatusRetirada.PREPARANDO));

        verify(sseHub, never()).publicar(any(), anyString(), anyString(), any());
        verify(consultaService, never()).porId(any(), any());
    }

    @Test
    @DisplayName("O payload leva chave de foto, nunca bytes de imagem")
    void payloadNaoCarregaBytesDeImagem() {
        UUID painelDaSala = UUID.randomUUID();
        when(fonteRepository.paineisQueCobrem(any(), any(), any(), any(), any()))
                .thenReturn(List.of(painelDaSala));
        when(painelRepository.findByTenantIdAndUnitIdAndTipoAndAtivoTrueAndDeletedFalse(any(), any(), any()))
                .thenReturn(List.of());
        when(consultaService.porId(any(), any())).thenReturn(cartao());

        publicador.publicar(evento(TENANT_A, StatusRetirada.PRONTO, StatusRetirada.ENTREGUE));

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(sseHub).publicar(any(), anyString(), eq("retirada.entregue"), payload.capture());
        RetiradaFilaItem item = (RetiradaFilaItem) payload.getValue();
        assertTrue(item.aluno().fotoKey().startsWith("faces/"));
        assertFalse(item.aluno().fotoKey().startsWith("data:"), "chave de storage, nunca base64");
        assertEquals("Joao", item.aluno().nome());
    }

    @Test
    @DisplayName("Cada status vira o evento SSE que a TV escuta")
    void nomesDosEventosSse() {
        assertEquals("retirada.aberta", evento(TENANT_A, null, StatusRetirada.SOLICITADA).nomeSse());
        assertEquals("retirada.preparando", evento(TENANT_A, null, StatusRetirada.PREPARANDO).nomeSse());
        assertEquals("retirada.pronto", evento(TENANT_A, null, StatusRetirada.PRONTO).nomeSse());
        assertEquals("retirada.entregue", evento(TENANT_A, null, StatusRetirada.ENTREGUE).nomeSse());
        assertEquals("retirada.cancelada", evento(TENANT_A, null, StatusRetirada.CANCELADA).nomeSse());
        assertEquals("retirada.cancelada", evento(TENANT_A, null, StatusRetirada.NEGADA).nomeSse());
    }

    @Test
    @DisplayName("Falha na publicacao nao escala: a TV se recupera sozinha")
    void falhaNaPublicacaoNaoEscala() {
        when(fonteRepository.paineisQueCobrem(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("banco fora do ar"));

        publicador.aoMudarStatus(evento(TENANT_A, StatusRetirada.SOLICITADA, StatusRetirada.PREPARANDO));

        verify(sseHub, never()).publicar(any(), anyString(), anyString(), any());
    }
}
