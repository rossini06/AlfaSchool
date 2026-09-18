package br.com.alfaschool.backend.access.retirada;

import br.com.alfaschool.backend.application.access.retirada.OcorrenciaService;
import br.com.alfaschool.backend.application.access.retirada.RegistrarOcorrenciaEvent;
import br.com.alfaschool.backend.application.access.retirada.dto.TratativaRequest;
import br.com.alfaschool.backend.application.access.shared.NotificacaoPort;
import br.com.alfaschool.backend.domain.access.retirada.AccOcorrencia;
import br.com.alfaschool.backend.domain.access.retirada.GravidadeOcorrencia;
import br.com.alfaschool.backend.domain.access.retirada.StatusOcorrencia;
import br.com.alfaschool.backend.domain.access.shared.TipoOcorrencia;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccOcorrenciaRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import br.com.alfaschool.backend.security.jwt.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OcorrenciaServiceTest {

    private static final UUID TENANT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID UNIDADE = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USUARIO = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock private AccOcorrenciaRepository ocorrenciaRepository;
    @Mock private NotificacaoPort notificacaoPort;

    private OcorrenciaService service;

    @BeforeEach
    void preparar() {
        service = new OcorrenciaService(ocorrenciaRepository, RetiradaServiceTest.provedorDe(notificacaoPort));
        TenantContext.setTenantId(TENANT);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(USUARIO, TENANT, UNIDADE, List.of("COORDENADOR")), null, List.of()));
        when(ocorrenciaRepository.save(any(AccOcorrencia.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void limpar() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private AccOcorrencia aberta() {
        AccOcorrencia o = new AccOcorrencia();
        o.setTenantId(TENANT);
        o.setUnitId(UNIDADE);
        o.setTipo(TipoOcorrencia.TENTATIVA_NAO_AUTORIZADA);
        o.setGravidade(GravidadeOcorrencia.ALTA);
        o.setDescricao("Pessoa sem autorizacao na portaria");
        o.setStatus(StatusOcorrencia.ABERTA);
        return o;
    }

    @Test
    @DisplayName("Registrar grava e notifica a coordenacao")
    void registrarGravaENotifica() {
        AccOcorrencia salva = service.registrar(new RegistrarOcorrenciaEvent(TENANT, UNIDADE,
                TipoOcorrencia.TENTATIVA_NAO_AUTORIZADA, GravidadeOcorrencia.ALTA, null,
                UUID.randomUUID(), null, null, null, "Tentativa na portaria"));

        assertNotNull(salva);
        assertEquals(StatusOcorrencia.ABERTA, salva.getStatus());
        verify(notificacaoPort).enfileirar(eq(TENANT), any(), any(), any(), any(), anyMap(), anyString());
    }

    @Test
    @DisplayName("Falha ao gravar nunca derruba quem chamou")
    void registrarNuncaLanca() {
        when(ocorrenciaRepository.save(any())).thenThrow(new IllegalStateException("banco fora"));

        assertNull(service.registrar(new RegistrarOcorrenciaEvent(TENANT, UNIDADE,
                TipoOcorrencia.EQUIPAMENTO_OFFLINE, GravidadeOcorrencia.MEDIA, null, null, null,
                null, null, "Catraca offline")));
    }

    @Test
    @DisplayName("Notificacao que falha nao desfaz a ocorrencia")
    void notificacaoQueFalhaNaoPerdeOcorrencia() {
        doThrow(new IllegalStateException("fila indisponivel"))
                .when(notificacaoPort).enfileirar(any(), any(), any(), any(), any(), anyMap(), anyString());

        AccOcorrencia salva = service.registrar(new RegistrarOcorrenciaEvent(TENANT, UNIDADE,
                TipoOcorrencia.RETIRADA_MANUAL, GravidadeOcorrencia.MEDIA, null, null, null,
                null, null, "Retirada manual"));

        assertNotNull(salva, "a ocorrencia e a prova do problema: nao pode se perder com a notificacao");
    }

    @Test
    @DisplayName("Tratar exige usuario identificado, tratativa e carimba o momento")
    void tratarExigeUsuarioETratativa() {
        UUID id = UUID.randomUUID();
        AccOcorrencia ocorrencia = aberta();
        when(ocorrenciaRepository.findByIdAndTenantIdAndDeletedFalse(id, TENANT))
                .thenReturn(Optional.of(ocorrencia));

        ResponseStatusException semTratativa = assertThrows(ResponseStatusException.class,
                () -> service.tratar(id, new TratativaRequest("  ")));
        assertEquals(HttpStatus.BAD_REQUEST, semTratativa.getStatusCode());

        service.tratar(id, new TratativaRequest("Contatei a mae por telefone"));

        assertEquals(StatusOcorrencia.EM_TRATATIVA, ocorrencia.getStatus());
        assertEquals(USUARIO, ocorrencia.getTratadoPorUserId());
        assertNotNull(ocorrencia.getTratadoEm());
    }

    @Test
    void tratarSemUsuarioEhRejeitado() {
        UUID id = UUID.randomUUID();
        when(ocorrenciaRepository.findByIdAndTenantIdAndDeletedFalse(id, TENANT))
                .thenReturn(Optional.of(aberta()));
        SecurityContextHolder.clearContext();

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.tratar(id, new TratativaRequest("qualquer coisa")));
        assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatusCode());
    }

    @Test
    @DisplayName("Nao se fecha ocorrencia que ninguem tratou")
    void fecharExigeTratativa() {
        UUID id = UUID.randomUUID();
        AccOcorrencia ocorrencia = aberta();
        when(ocorrenciaRepository.findByIdAndTenantIdAndDeletedFalse(id, TENANT))
                .thenReturn(Optional.of(ocorrencia));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> service.fechar(id));
        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());

        ocorrencia.setTratadoPorUserId(USUARIO);
        ocorrencia.setTratativa("Resolvido com a familia");
        ocorrencia.setTratadoEm(Instant.now());
        service.fechar(id);
        assertEquals(StatusOcorrencia.FECHADA, ocorrencia.getStatus());
    }

    @Test
    void fecharDuasVezesDevolve409() {
        UUID id = UUID.randomUUID();
        AccOcorrencia ocorrencia = aberta();
        ocorrencia.setStatus(StatusOcorrencia.FECHADA);
        when(ocorrenciaRepository.findByIdAndTenantIdAndDeletedFalse(id, TENANT))
                .thenReturn(Optional.of(ocorrencia));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> service.fechar(id));
        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    @Test
    @DisplayName("Ocorrencia de outro tenant nao e encontrada")
    void outroTenantNaoEnxerga() {
        UUID id = UUID.randomUUID();
        when(ocorrenciaRepository.findByIdAndTenantIdAndDeletedFalse(id, TENANT)).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> service.buscar(id));
        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
    }
}
