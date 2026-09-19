package br.com.alfaschool.backend.shared.exception;

import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.UUID;

/**
 * Traducao de excecao em resposta HTTP.
 *
 * <h2>Dois defeitos que este arquivo ja' teve</h2>
 * 1. O {@code @ExceptionHandler(Exception.class)} engolia TUDO, inclusive
 *    "rota nao encontrada" e "metodo nao suportado" — entao qualquer 404 ou
 *    405 chegava no navegador como 500. O frontend tem logica de degradacao
 *    que reage a 404/405 e ela nunca disparava: as telas mostravam "erro
 *    interno" para funcionalidade simplesmente inexistente.
 *
 * 2. Nada era logado. Um 500 acontecia e nao sobrava rastro nenhum no log,
 *    o que torna qualquer diagnostico impossivel. Agora o erro inesperado
 *    sai com stack trace e um codigo de correlacao que tambem vai na
 *    resposta, para o usuario poder citar o codigo no suporte.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Object>> handleResponseStatus(ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        return ResponseEntity.status(status)
                .body(ApiResponse.of(status, exception.getReason() == null ? "Erro inesperado" : exception.getReason(), null));
    }

    /** Rota inexistente. Precisa ser 404 para o frontend distinguir de falha. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleRotaInexistente(NoResourceFoundException ex) {
        log.warn("Rota inexistente: {}", ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.of(404, "Recurso não encontrado.", null));
    }

    /** Caminho existe, verbo errado. 405 diz isso; 500 esconde. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMetodo(HttpRequestMethodNotSupportedException ex) {
        log.warn("Metodo {} nao suportado neste caminho. Suportados: {}",
                ex.getMethod(), ex.getSupportedHttpMethods());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.of(405, "Operação não suportada neste endereço.", null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? "Dados inválidos" : error.getDefaultMessage())
                .orElse("Dados inválidos");

        return ResponseEntity.badRequest().body(ApiResponse.of(HttpStatus.BAD_REQUEST.value(), message, null));
    }

    /** Parametro obrigatorio ausente: diga QUAL, senao quem chama fica adivinhando. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleParametroFaltando(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(
                ApiResponse.of(400, "Parâmetro obrigatório ausente: " + ex.getParameterName(), null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidJson(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.of(HttpStatus.BAD_REQUEST.value(), "JSON inválido. Verifique os campos informados.", null));
    }

    /** Id malformado na URL (ex.: UUID invalido) e' erro do cliente, nao do servidor. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTipoInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(
                ApiResponse.of(400, "Valor inválido para o parâmetro '" + ex.getName() + "'.", null));
    }

    /**
     * Violacao de restricao do banco: duplicidade, FK, campo obrigatorio.
     * A mensagem do banco NAO vai para o cliente — ela revela nome de tabela
     * e de coluna. Fica no log.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleIntegridade(DataIntegrityViolationException ex) {
        String codigo = UUID.randomUUID().toString().substring(0, 8);
        Throwable causa = ex.getMostSpecificCause();
        log.warn("Violacao de integridade [{}]: {}", codigo, causa.getMessage());

        // Truncamento NAO e' conflito. Responder 409 "conflita com dados
        // existentes" para um campo longo demais manda o usuario procurar
        // um registro duplicado que nao existe — ele mexe no nome, tenta de
        // novo, da' o mesmo erro, e desiste. O banco distingue os dois casos
        // pelo SQLState: 22001 e' tamanho, 23000 e' chave.
        String estado = causa instanceof java.sql.SQLException sql ? sql.getSQLState() : null;
        if ("22001".equals(estado)) {
            String campo = nomeDaColuna(causa.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.of(400,
                    campo == null
                            ? "Um dos campos enviados é maior que o limite permitido."
                            : "O campo '" + campo + "' é maior que o limite permitido.", null));
        }
        if (estado != null && estado.startsWith("23")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.of(409,
                    "Já existe um registro com esses dados. Código: " + codigo, null));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.of(409,
                "A operação conflita com dados existentes. Código: " + codigo, null));
    }

    /**
     * Extrai so' o nome da coluna de "Data too long for column 'cidade' at
     * row 1". O resto da mensagem do banco fica no log: ela revela nome de
     * tabela e estrutura interna.
     */
    private static String nomeDaColuna(String mensagemDoBanco) {
        if (mensagemDoBanco == null) {
            return null;
        }
        java.util.regex.Matcher m = COLUNA.matcher(mensagemDoBanco);
        return m.find() ? m.group(1) : null;
    }

    private static final java.util.regex.Pattern COLUNA =
            java.util.regex.Pattern.compile("column '([^']+)'");

    /**
     * Permissao insuficiente vira 403, nao 500.
     *
     * O accessDeniedHandler do SecurityConfig so' cobre o que o FILTRO
     * barra. O que o @PreAuthorize nega acontece depois, dentro do
     * controller.
     */
    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<ApiResponse<Object>> handleAcessoNegado(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiResponse.of(403, "Seu perfil não tem permissão para esta ação.", null));
    }

    /**
     * Ultimo recurso. Loga com stack trace e um codigo de correlacao que
     * tambem vai na resposta: sem ele, quem relata o problema nao tem como
     * dizer QUAL erro aconteceu, e quem investiga nao tem como achar.
     *
     * A mensagem ao cliente permanece generica de proposito — detalhe de
     * excecao revela estrutura interna.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception exception, HttpServletRequest request) {
        String codigo = UUID.randomUUID().toString().substring(0, 8);
        log.error("Erro inesperado [{}] em {} {}", codigo, request.getMethod(), request.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.of(500, "Erro interno ao processar a solicitação. Código: " + codigo, null));
    }
}
