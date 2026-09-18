package br.com.alfaschool.backend.application.access.simulador;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Estado em memoria do leitor falso.
 *
 * Existe para que o ControlIdClient possa ser exercitado fim a fim sem
 * hardware: sessoes, usuarios, vinculos e access_logs se comportam como
 * no firmware, inclusive nas respostas erradas (400 para duplicado, 200
 * com success=false para foto recusada).
 *
 * Tudo volatil de proposito. Reiniciar o backend e' a forma de "limpar o
 * historico do equipamento" — cenario que a deduplicacao precisa
 * sobreviver e que este simulador consegue reproduzir.
 */
@Component
@ConditionalOnProperty(name = "app.access.simulador-habilitado", havingValue = "true")
public class FirmwareFakeState {

    /** Sessoes emitidas por /login.fcgi. */
    public final Set<String> sessoes = ConcurrentHashMap.newKeySet();

    /** users: id -> nome. */
    public final Map<Long, String> usuarios = new ConcurrentHashMap<>();

    /** user_groups: user_id -> group_id. */
    public final Map<Long, Integer> vinculos = new ConcurrentHashMap<>();

    /** user_id -> hash da ultima foto aceita. */
    public final Map<Long, String> fotos = new ConcurrentHashMap<>();

    /** access_logs do equipamento, em ordem de insercao. */
    public final java.util.List<Map<String, Object>> accessLogs =
            java.util.Collections.synchronizedList(new java.util.ArrayList<>());

    public final AtomicLong proximoLogId = new AtomicLong(1);

    /**
     * Codigo de recusa que o /user_set_image.fcgi deve devolver na
     * proxima chamada. Null = aceitar. Ajustavel pelo endpoint de teste,
     * que e' como se reproduz "olhos fechados" sem um aluno de olhos
     * fechados na frente da camera.
     */
    public volatile Integer proximoErroDeFoto;

    /** Simula equipamento antigo que nao conhece destroy_objects.fcgi. */
    public volatile boolean firmwareAntigo = false;

    public void limpar() {
        sessoes.clear();
        usuarios.clear();
        vinculos.clear();
        fotos.clear();
        accessLogs.clear();
        proximoLogId.set(1);
        proximoErroDeFoto = null;
        firmwareAntigo = false;
    }
}
