package br.com.alfaschool.backend.application.access.painel.dto;

/**
 * UNICA resposta que carrega o token em claro, e so' no momento da criacao.
 *
 * Nao existe endpoint para recuperar o token depois: o banco so' tem o
 * hash. TV perdida ou token extraviado se resolve revogando e gerando
 * outro, que e' o comportamento que se quer.
 */
public record PainelDispositivoCriadoResponse(
        PainelDispositivoResponse dispositivo,
        String token,
        String aviso
) {
    public static PainelDispositivoCriadoResponse de(PainelDispositivoResponse dispositivo, String token) {
        return new PainelDispositivoCriadoResponse(dispositivo, token,
                "Guarde este token agora: ele nao podera ser exibido novamente.");
    }
}
