package br.com.alfaschool.backend.shared.exception;

import br.com.alfaschool.backend.shared.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Object>> handleResponseStatus(ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        return ResponseEntity.status(status)
                .body(ApiResponse.of(status, exception.getReason() == null ? "Erro inesperado" : exception.getReason(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? "Dados inválidos" : error.getDefaultMessage())
                .orElse("Dados inválidos");

        return ResponseEntity.badRequest().body(ApiResponse.of(HttpStatus.BAD_REQUEST.value(), message, null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidJson(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.of(HttpStatus.BAD_REQUEST.value(), "JSON inválido. Verifique os campos informados.", null));
    }

    /**
     * Id malformado na URL (ex.: UUID invalido). Sem este tratamento o
     * Spring deixava estourar como erro generico e a API devolvia 500 para
     * o que e' claramente erro do cliente.
     */
    /**
     * Permissao insuficiente vira 403, nao 500.
     *
     * O accessDeniedHandler do SecurityConfig so' cobre o que o FILTRO
     * barra. O que o @PreAuthorize nega acontece depois, ja' dentro do
     * controller, e sem este tratamento estourava como erro generico: a
     * tela dizia "erro interno" para o que e', na verdade, uma resposta
     * correta do sistema.
     */
    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<ApiResponse<Object>> handleAcessoNegado(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiResponse.of(403, "Seu perfil não tem permissão para esta ação.", null));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTipoInvalido(MethodArgumentTypeMismatchException ex) {
        String nome = ex.getName();
        return ResponseEntity.badRequest().body(
                ApiResponse.of(400, "Valor invalido para o parametro '" + nome + "'.", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Erro interno ao processar a solicitação.", null));
    }
}
