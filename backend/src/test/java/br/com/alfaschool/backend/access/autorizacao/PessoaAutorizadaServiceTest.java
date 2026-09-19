package br.com.alfaschool.backend.access.autorizacao;

import br.com.alfaschool.backend.application.access.autorizacao.CpfUtils;
import br.com.alfaschool.backend.application.access.autorizacao.PessoaAutorizadaService;
import br.com.alfaschool.backend.application.access.autorizacao.dto.PessoaAutorizadaRequest;
import br.com.alfaschool.backend.application.access.autorizacao.dto.PessoaAutorizadaResponse;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccPessoaAutorizadaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.ResponsavelRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PessoaAutorizadaServiceTest {

    private static final UUID TENANT = UUID.randomUUID();

    @Mock private AccPessoaAutorizadaRepository pessoaRepository;
    @Mock private ResponsavelRepository responsavelRepository;

    private PessoaAutorizadaService service;

    @BeforeEach
    void setUp() {
        service = new PessoaAutorizadaService(pessoaRepository, responsavelRepository);
        TenantContext.setTenantId(TENANT);
        when(pessoaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("as tres permissoes sao independentes: portal e notificacao nao ligam pode_retirar")
    void permissoesIndependentes() {
        PessoaAutorizadaResponse resposta = service.create(new PessoaAutorizadaRequest(
                null, "Avo Rita", "Avo", "529.982.247-25", null, null, null, null, null,
                false, true, true, true));

        assertThat(resposta.podeRetirar()).isFalse();
        assertThat(resposta.podeAcessarPortal()).isTrue();
        assertThat(resposta.recebeNotificacao()).isTrue();
    }

    @Test
    @DisplayName("quem pode retirar nao ganha portal nem notificacao por tabela")
    void retirarNaoConcedePortal() {
        PessoaAutorizadaResponse resposta = service.create(new PessoaAutorizadaRequest(
                null, "Avo Rita", null, null, null, null, null, null, null,
                true, null, null, null));

        assertThat(resposta.podeRetirar()).isTrue();
        assertThat(resposta.podeAcessarPortal()).isFalse();
        assertThat(resposta.recebeNotificacao()).isFalse();
    }

    @Test
    @DisplayName("CPF invalido e recusado")
    void cpfInvalidoRecusado() {
        assertThatThrownBy(() -> service.create(new PessoaAutorizadaRequest(
                null, "Fulano", null, "111.111.111-11", null, null, null, null, null, true, null, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("CPF inválido");
    }

    @Test
    @DisplayName("CPF duplicado no mesmo tenant e recusado")
    void cpfDuplicadoRecusado() {
        when(pessoaRepository.existsByTenantIdAndCpfAndDeletedFalse(TENANT, "52998224725")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new PessoaAutorizadaRequest(
                null, "Fulano", null, "52998224725", null, null, null, null, null, true, null, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("CPF e' persistido so com digitos para casar com restricao mascarada")
    void cpfNormalizado() {
        PessoaAutorizadaResponse resposta = service.create(new PessoaAutorizadaRequest(
                null, "Fulano", null, "529.982.247-25", null, null, null, null, null, true, null, null, null));

        assertThat(resposta.cpf()).isEqualTo("52998224725");
    }

    @Test
    @DisplayName("sem tenant no contexto nada e' criado")
    void semTenantRecusa() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.create(new PessoaAutorizadaRequest(
                null, "Fulano", null, null, null, null, null, null, null, true, null, null, null)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("validacao de CPF cobre digito verificador e sequencias repetidas")
    void validacaoDeCpf() {
        assertThat(CpfUtils.valido("529.982.247-25")).isTrue();
        assertThat(CpfUtils.valido("39053344705")).isTrue();
        assertThat(CpfUtils.valido("52998224724")).isFalse();
        assertThat(CpfUtils.valido("00000000000")).isFalse();
        assertThat(CpfUtils.valido("123")).isFalse();
        assertThat(CpfUtils.valido(null)).isFalse();
    }
}
