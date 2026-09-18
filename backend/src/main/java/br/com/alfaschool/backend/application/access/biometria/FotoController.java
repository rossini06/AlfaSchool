package br.com.alfaschool.backend.application.access.biometria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entrega a foto de referencia para uma tag {@code <img>}.
 *
 * Nao exige JWT — e nao poderia, porque {@code <img>} nao manda
 * cabecalho e a TV do painel nao faz login. Quem autoriza e' a assinatura
 * na propria URL, emitida por quem ja estava autenticado. Ver
 * {@link FotoUrlAssinada}.
 *
 * A resposta e' marcada como {@code private, no-store}: foto de crianca
 * nao deve ficar em cache de proxy nem em disco do navegador.
 */
@RestController
@RequestMapping("/api/v1/access/fotos")
public class FotoController {

    private static final Logger log = LoggerFactory.getLogger(FotoController.class);

    private final FotoStorage storage;
    private final FotoUrlAssinada urlAssinada;

    public FotoController(FotoStorage storage, FotoUrlAssinada urlAssinada) {
        this.storage = storage;
        this.urlAssinada = urlAssinada;
    }

    @GetMapping("/{chave}")
    public ResponseEntity<byte[]> servir(@PathVariable String chave,
                                         @RequestParam(required = false) String exp,
                                         @RequestParam(required = false) String sig) {
        if (!urlAssinada.valida(chave, exp, sig)) {
            // 404 e nao 403: um 403 confirmaria que aquela foto existe, o
            // que ja e' informacao demais para quem esta sondando chaves.
            log.warn("Tentativa de acesso a foto com assinatura invalida ou vencida");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (!storage.existe(chave)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        byte[] conteudo;
        try {
            conteudo = storage.ler(chave);
        } catch (RuntimeException e) {
            log.warn("Falha ao ler a foto {}: {}", chave, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok()
                .contentType(tipoDe(conteudo))
                .cacheControl(CacheControl.noStore().cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(conteudo);
    }

    /** Pelos magic bytes; nao confiamos em extensao vinda da chave. */
    private MediaType tipoDe(byte[] conteudo) {
        if (conteudo != null && conteudo.length > 3
                && (conteudo[0] & 0xFF) == 0x89 && conteudo[1] == 'P'
                && conteudo[2] == 'N' && conteudo[3] == 'G') {
            return MediaType.IMAGE_PNG;
        }
        return MediaType.IMAGE_JPEG;
    }
}
