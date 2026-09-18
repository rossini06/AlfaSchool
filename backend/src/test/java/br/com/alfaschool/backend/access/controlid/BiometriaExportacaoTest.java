package br.com.alfaschool.backend.access.controlid;

import br.com.alfaschool.backend.application.access.biometria.DeviceUserIdService;
import br.com.alfaschool.backend.application.access.biometria.ExportacaoBiometriaBloqueadaException;
import br.com.alfaschool.backend.application.access.biometria.FaceService;
import br.com.alfaschool.backend.application.access.biometria.FotoStorage;
import br.com.alfaschool.backend.application.access.controlid.ControlIdClient;
import br.com.alfaschool.backend.application.access.controlid.FotoRecusadaException;
import br.com.alfaschool.backend.domain.access.biometria.AccDeviceUserSeq;
import br.com.alfaschool.backend.domain.access.biometria.AccFace;
import br.com.alfaschool.backend.domain.access.biometria.AccFaceSync;
import br.com.alfaschool.backend.domain.access.shared.StatusFaceSync;
import br.com.alfaschool.backend.domain.access.shared.TitularTipo;
import br.com.alfaschool.backend.domain.dispositivo.Dispositivo;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccDeviceUserSeqRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccFaceRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccFaceSyncRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.DispositivoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exportacao de biometria para o equipamento.
 *
 * Duas regras que nao podem regredir:
 *  - sem base legal ou sem consentimento a foto NAO sai (LGPD Art. 11/14);
 *  - o device_user_id vem de sequencia por tenant, entao dois tenants
 *    nunca disputam o mesmo numero dentro do leitor.
 */
class BiometriaExportacaoTest {

    private AccFaceRepository faces;
    private AccFaceSyncRepository sincronizacoes;
    private DispositivoRepository dispositivos;
    private FotoStorage fotos;
    private DeviceUserIdService deviceUserIds;
    private ControlIdClient client;
    private FaceService service;

    private UUID tenantId;
    private UUID dispositivoId;
    private Dispositivo dispositivo;
    private final List<AccFaceSync> salvos = new ArrayList<>();

    /** JPEG minimo valido: magic bytes corretos e tamanho acima do piso. */
    private static byte[] jpegDeTeste() {
        byte[] b = new byte[128];
        b[0] = (byte) 0xFF;
        b[1] = (byte) 0xD8;
        b[2] = (byte) 0xFF;
        return b;
    }

    @BeforeEach
    void preparar() {
        faces = mock(AccFaceRepository.class);
        sincronizacoes = mock(AccFaceSyncRepository.class);
        dispositivos = mock(DispositivoRepository.class);
        fotos = mock(FotoStorage.class);
        deviceUserIds = mock(DeviceUserIdService.class);
        client = mock(ControlIdClient.class);
        salvos.clear();

        service = new FaceService(faces, sincronizacoes, dispositivos, fotos, deviceUserIds, client);

        tenantId = UUID.randomUUID();
        dispositivoId = UUID.randomUUID();

        dispositivo = new Dispositivo();
        ReflectionTestUtils.setField(dispositivo, "id", dispositivoId);
        dispositivo.setTenantId(tenantId);
        dispositivo.setDeleted(false);
        dispositivo.setNome("Catraca Entrada");
        when(dispositivos.findById(dispositivoId)).thenReturn(Optional.of(dispositivo));

        when(sincronizacoes.findByFaceIdAndDispositivoId(any(), any())).thenReturn(Optional.empty());
        when(sincronizacoes.save(any())).thenAnswer(inv -> {
            AccFaceSync s = inv.getArgument(0);
            salvos.add(s);
            return s;
        });
        when(fotos.ler(any())).thenReturn(jpegDeTeste());
    }

    private AccFace face(String baseLegal, boolean consentimento) {
        AccFace f = new AccFace();
        UUID id = UUID.randomUUID();
        ReflectionTestUtils.setField(f, "id", id);
        f.setTenantId(tenantId);
        f.setDeleted(false);
        f.setTitularTipo(TitularTipo.ALUNO);
        f.setTitularId(UUID.randomUUID());
        f.setDeviceUserId(1042L);
        f.setFotoKey(tenantId + "/foto.jpg");
        f.setAtivo(true);
        f.setBaseLegal(baseLegal);
        f.setConsentimentoObtido(consentimento);
        if (consentimento) {
            f.setConsentimentoEm(Instant.now());
        }
        when(faces.findByIdAndTenantIdAndDeletedFalse(id, tenantId)).thenReturn(Optional.of(f));
        return f;
    }

    // =================================================================
    // LGPD
    // =================================================================

    @Test
    @DisplayName("face sem base legal NAO e' exportada e a recusa fica registrada")
    void semBaseLegalNaoExporta() {
        AccFace f = face(null, true);

        assertThatThrownBy(() -> service.sincronizar(tenantId, f.getId(), dispositivoId))
                .isInstanceOf(ExportacaoBiometriaBloqueadaException.class)
                .hasMessageContaining("base legal");

        verify(client, never()).enviarFoto(any(), anyLong(), any());
        verify(client, never()).sincronizarUsuario(any(), any());
        // A auditoria de LGPD precisa ver que o sistema barrou.
        assertThat(salvos).hasSize(1);
        assertThat(salvos.get(0).getStatus()).isEqualTo(StatusFaceSync.RECUSADA);
        assertThat(salvos.get(0).getCodigoErro()).isEqualTo("SEM_BASE_LEGAL");
    }

    @Test
    @DisplayName("face sem consentimento NAO e' exportada")
    void semConsentimentoNaoExporta() {
        AccFace f = face("TUTELA_DO_MENOR", false);

        assertThatThrownBy(() -> service.sincronizar(tenantId, f.getId(), dispositivoId))
                .isInstanceOf(ExportacaoBiometriaBloqueadaException.class)
                .hasMessageContaining("consentimento");

        verify(client, never()).enviarFoto(any(), anyLong(), any());
        assertThat(salvos.get(0).getCodigoErro()).isEqualTo("SEM_CONSENTIMENTO");
    }

    @Test
    @DisplayName("base legal em branco conta como ausente")
    void baseLegalEmBrancoNaoVale() {
        AccFace f = face("   ", true);

        assertThat(f.exportavel()).isFalse();
        assertThat(FaceService.motivoDeBloqueio(f)).isEqualTo("SEM_BASE_LEGAL");
    }

    @Test
    @DisplayName("com base legal e consentimento a foto vai para o equipamento")
    void comBaseLegalEConsentimentoExporta() {
        AccFace f = face("TUTELA_DO_MENOR", true);

        AccFaceSync sync = service.sincronizar(tenantId, f.getId(), dispositivoId);

        verify(client).sincronizarUsuario(eq(dispositivo), any());
        verify(client).enviarFoto(eq(dispositivo), eq(1042L), any());
        assertThat(sync.getStatus()).isEqualTo(StatusFaceSync.ACEITA);
        assertThat(sync.getFotoHash()).isNotBlank();
    }

    // =================================================================
    // Veredito por equipamento
    // =================================================================

    @Test
    @DisplayName("recusa do leitor vira RECUSADA com motivo legivel, sem guardar o hash")
    void recusaDoLeitorEhRegistrada() {
        AccFace f = face("TUTELA_DO_MENOR", true);
        doThrow(new FotoRecusadaException("8", "Foto sem nitidez (borrada) ou de baixa qualidade."))
                .when(client).enviarFoto(any(), anyLong(), any());

        AccFaceSync sync = service.sincronizar(tenantId, f.getId(), dispositivoId);

        assertThat(sync.getStatus()).isEqualTo(StatusFaceSync.RECUSADA);
        assertThat(sync.getCodigoErro()).isEqualTo("8");
        assertThat(sync.getDetalhe()).contains("nitidez");
        // Sem hash gravado, a mesma foto nao sera considerada "ja enviada".
        assertThat(sync.getFotoHash()).isNull();
    }

    @Test
    @DisplayName("mesma foto ja aceita nao e' reenviada")
    void fotoIdenticaNaoEhReenviada() {
        AccFace f = face("TUTELA_DO_MENOR", true);
        AccFaceSync existente = new AccFaceSync();
        existente.setTenantId(tenantId);
        existente.setFaceId(f.getId());
        existente.setDispositivoId(dispositivoId);
        existente.setStatus(StatusFaceSync.ACEITA);
        existente.setFotoHash(FaceService.hashDaFoto(jpegDeTeste()));
        when(sincronizacoes.findByFaceIdAndDispositivoId(f.getId(), dispositivoId))
                .thenReturn(Optional.of(existente));

        AccFaceSync sync = service.sincronizar(tenantId, f.getId(), dispositivoId);

        assertThat(sync.getStatus()).isEqualTo(StatusFaceSync.ACEITA);
        verify(client, never()).enviarFoto(any(), anyLong(), any());
    }

    // =================================================================
    // Sequencia de device_user_id
    // =================================================================

    @Test
    @DisplayName("sequencia de device_user_id e' independente por tenant e nao colide")
    void sequenciaPorTenantNaoColide() {
        AccDeviceUserSeqRepository repo = mock(AccDeviceUserSeqRepository.class);
        Map<UUID, AccDeviceUserSeq> banco = new HashMap<>();
        when(repo.travarPorTenant(any()))
                .thenAnswer(inv -> Optional.ofNullable(banco.get(inv.<UUID>getArgument(0))));
        when(repo.save(any())).thenAnswer(inv -> {
            AccDeviceUserSeq s = inv.getArgument(0);
            banco.put(s.getTenantId(), s);
            return s;
        });
        DeviceUserIdService seq = new DeviceUserIdService(repo);

        UUID escolaA = UUID.randomUUID();
        UUID escolaB = UUID.randomUUID();

        long a1 = seq.proximo(escolaA);
        long a2 = seq.proximo(escolaA);
        long b1 = seq.proximo(escolaB);
        long a3 = seq.proximo(escolaA);

        // Dentro do tenant a sequencia avanca e nunca repete.
        assertThat(List.of(a1, a2, a3)).doesNotHaveDuplicates();
        assertThat(a2).isEqualTo(a1 + 1);
        assertThat(a3).isEqualTo(a2 + 1);
        // O contador de B nao foi contaminado pelo de A: cada tenant tem o
        // proprio espaco de numeracao, que e' o que a UNIQUE
        // (tenant_id, device_user_id) protege.
        assertThat(b1).isEqualTo(DeviceUserIdService.PRIMEIRO_ID);
        assertThat(banco.get(escolaA).getProximoId())
                .isNotEqualTo(banco.get(escolaB).getProximoId());
    }

    @Test
    @DisplayName("primeiro id de um tenant novo comeca longe dos usuarios de fabrica")
    void primeiroIdDeTenantNovo() {
        AccDeviceUserSeqRepository repo = mock(AccDeviceUserSeqRepository.class);
        when(repo.travarPorTenant(any())).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        long primeiro = new DeviceUserIdService(repo).proximo(UUID.randomUUID());

        assertThat(primeiro).isEqualTo(DeviceUserIdService.PRIMEIRO_ID);
        assertThat(primeiro).isGreaterThan(1L);
    }
}
