package br.com.alfaschool.backend.access.autorizacao;

import br.com.alfaschool.backend.application.access.autorizacao.AutorizacaoExpiracaoJob;
import br.com.alfaschool.backend.domain.access.autorizacao.AcaoAutorizacao;
import br.com.alfaschool.backend.domain.access.autorizacao.AutorizacaoHistorico;
import br.com.alfaschool.backend.domain.access.autorizacao.AutorizacaoRetirada;
import br.com.alfaschool.backend.domain.access.shared.StatusAutorizacao;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccAutorizacaoHistoricoRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccAutorizacaoRetiradaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static br.com.alfaschool.backend.access.autorizacao.AutorizacaoTestFixtures.autorizacao;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutorizacaoExpiracaoJobTest {

    private static final UUID TENANT = UUID.randomUUID();

    @Mock private AccAutorizacaoRetiradaRepository autorizacaoRepository;
    @Mock private AccAutorizacaoHistoricoRepository historicoRepository;

    @InjectMocks private AutorizacaoExpiracaoJob job;

    @Test
    @DisplayName("ATIVA com vigencia vencida vira EXPIRADA e grava historico do sistema")
    void expiraVencidas() {
        AutorizacaoRetirada vencida = autorizacao(
                UUID.randomUUID(), TENANT, UUID.randomUUID(), UUID.randomUUID(), StatusAutorizacao.ATIVA);
        vencida.setPermanente(false);
        vencida.setVigenciaFim(LocalDate.now().minusDays(3));

        when(autorizacaoRepository.findByStatusAndVigenciaFimBeforeAndDeletedFalse(
                eq(StatusAutorizacao.ATIVA), any())).thenReturn(List.of(vencida));

        job.expirarVencidas();

        assertThat(vencida.getStatus()).isEqualTo(StatusAutorizacao.EXPIRADA);

        ArgumentCaptor<AutorizacaoHistorico> captor = ArgumentCaptor.forClass(AutorizacaoHistorico.class);
        verify(historicoRepository).save(captor.capture());
        AutorizacaoHistorico historico = captor.getValue();
        assertThat(historico.getAcao()).isEqualTo(AcaoAutorizacao.EXPIRACAO);
        assertThat(historico.getStatusAnterior()).isEqualTo(StatusAutorizacao.ATIVA);
        assertThat(historico.getStatusNovo()).isEqualTo(StatusAutorizacao.EXPIRADA);
        // Acao do sistema: nao ha usuario a quem atribuir.
        assertThat(historico.getUserId()).isNull();
        assertThat(historico.getMotivo()).isEqualTo("Expiracao automatica");
    }

    @Test
    @DisplayName("sem vencidas nao grava nada")
    void semVencidasNaoGrava() {
        when(autorizacaoRepository.findByStatusAndVigenciaFimBeforeAndDeletedFalse(
                eq(StatusAutorizacao.ATIVA), any())).thenReturn(List.of());

        job.expirarVencidas();

        verify(autorizacaoRepository, never()).save(any());
        verify(historicoRepository, never()).save(any());
    }
}
