package br.com.alfaschool.backend.application.access.biometria.dto;

import br.com.alfaschool.backend.domain.access.shared.TitularTipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Cadastro de face.
 *
 * A foto vem em base64 para caber num unico POST JSON junto do bloco de
 * consentimento. Separar os dois em chamadas distintas abriria a janela
 * em que a foto ja esta gravada e o consentimento ainda nao — e e'
 * exatamente essa janela que a LGPD nao perdoa.
 */
public record CadastroFaceRequest(
        @NotNull(message = "Tipo de titular é obrigatório.") TitularTipo titularTipo,
        @NotNull(message = "Titular é obrigatório.") UUID titularId,
        @NotBlank(message = "Foto é obrigatória.") String fotoBase64,
        /** Hipotese do Art. 7/11 que autoriza o tratamento. Sem ela a face nao vai para o leitor. */
        String baseLegal,
        boolean consentimentoObtido,
        String consentimentoVersao,
        String consentimentoOrigem,
        String consentimentoPor
) {
}
