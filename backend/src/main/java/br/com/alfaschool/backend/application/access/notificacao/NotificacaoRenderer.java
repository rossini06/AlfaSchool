package br.com.alfaschool.backend.application.access.notificacao;

import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import br.com.alfaschool.backend.domain.access.shared.EventoNotificacao;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renderiza o texto do aviso e aplica a restricao de privacidade sobre as
 * variaveis.
 *
 * <h2>RESTRICAO DE PRIVACIDADE (regra de negocio, nao detalhe tecnico)</h2>
 * Uma notificacao de acesso escolar NUNCA pode carregar foto, dado biometrico
 * ou detalhe excessivo da crianca. Motivos:
 * <ul>
 *   <li>O canal e' inseguro por natureza: e-mail e WhatsApp ficam em
 *       dispositivos compartilhados, backups em nuvem de terceiros e caixas de
 *       entrada de quem nao e' o responsavel.</li>
 *   <li>Dado biometrico e' dado pessoal sensivel (LGPD art. 5, II) e de
 *       crianca (art. 14). Trafega-lo em aviso de rotina nao tem base legal.</li>
 *   <li>Um template com {@code {foto}} vira, na pratica, um vazamento em
 *       massa: um aviso por entrada, todo dia, para varios destinatarios.</li>
 * </ul>
 * O aviso deve dizer o minimo util: nome do aluno, o que aconteceu, quando e
 * onde. Nada alem disso.
 *
 * <p>A validacao e' dupla: por NOME da variavel (lista de campos proibidos) e
 * por FORMATO do valor (data-URI ou base64 longo), porque um campo inocente
 * como {@code observacao} pode receber uma imagem inteira de quem chamou.
 */
@Component
public class NotificacaoRenderer {

    /** Nomes de variavel que nunca podem entrar num aviso. */
    private static final Set<String> VARIAVEIS_PROIBIDAS = Set.of(
            "foto", "photo", "imagem", "image", "avatar", "retrato",
            "biometria", "biometrico", "digital", "impressao_digital",
            "face", "facial", "template_biometrico", "rosto",
            "cpf_aluno", "rg_aluno", "endereco_aluno",
            "observacoes_medicas", "laudo", "prontuario"
    );

    private static final Pattern DATA_URI = Pattern.compile("^\\s*data:[^;,]*[;,]", Pattern.CASE_INSENSITIVE);
    private static final Pattern BASE64_LONGO = Pattern.compile("^[A-Za-z0-9+/_-]{256,}={0,2}$");
    private static final Pattern MARCADOR = Pattern.compile("\\{([a-zA-Z0-9_]+)}");

    /** Tamanho maximo de um valor de variavel. Aviso e' aviso, nao dossie. */
    private static final int TAM_MAX_VALOR = 300;

    /** Variavel reprovada na checagem de privacidade. */
    public static class VariavelProibidaException extends RuntimeException {
        public VariavelProibidaException(String mensagem) {
            super(mensagem);
        }
    }

    /**
     * Rejeita o conjunto inteiro se qualquer variavel violar a restricao.
     * Falha fechada de proposito: melhor nao avisar do que avisar vazando.
     */
    public void validarVariaveis(Map<String, String> variaveis) {
        if (variaveis == null || variaveis.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> e : variaveis.entrySet()) {
            String nome = e.getKey() == null ? "" : e.getKey().trim().toLowerCase(Locale.ROOT);
            String valor = e.getValue();

            if (VARIAVEIS_PROIBIDAS.contains(nome)) {
                throw new VariavelProibidaException(
                        "Variavel '" + nome + "' nao pode ser usada em notificacao: dado sensivel de aluno");
            }
            if (valor == null) {
                continue;
            }
            if (DATA_URI.matcher(valor).find()) {
                throw new VariavelProibidaException(
                        "Variavel '" + nome + "' contem data-URI (provavel imagem). Proibido em notificacao");
            }
            String semEspaco = valor.replaceAll("\\s+", "");
            if (BASE64_LONGO.matcher(semEspaco).matches()) {
                throw new VariavelProibidaException(
                        "Variavel '" + nome + "' parece conteudo base64 longo (foto/biometria). Proibido em notificacao");
            }
            if (valor.length() > TAM_MAX_VALOR) {
                throw new VariavelProibidaException(
                        "Variavel '" + nome + "' excede " + TAM_MAX_VALOR + " caracteres: detalhe excessivo em aviso");
            }
        }
    }

    /**
     * Substitui {@code {variavel}} pelo valor. Marcador sem valor vira string
     * vazia — nunca deixamos o texto cru chegar a familia com chaves soltas.
     */
    public String renderizar(String template, Map<String, String> variaveis) {
        if (template == null || template.isBlank()) {
            return "";
        }
        Map<String, String> fonte = variaveis == null ? Map.of() : normalizar(variaveis);
        Matcher m = MARCADOR.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String chave = m.group(1).toLowerCase(Locale.ROOT);
            String valor = fonte.getOrDefault(chave, "");
            m.appendReplacement(sb, Matcher.quoteReplacement(valor));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private Map<String, String> normalizar(Map<String, String> variaveis) {
        Map<String, String> out = new HashMap<>();
        variaveis.forEach((k, v) -> {
            if (k != null) {
                out.put(k.trim().toLowerCase(Locale.ROOT), v == null ? "" : v);
            }
        });
        return out;
    }

    /**
     * Texto de fabrica, usado quando o tenant nao cadastrou template. Evita o
     * pior cenario: evento acontece, ninguem cadastrou nada, familia nao e'
     * avisada e ninguem percebe.
     */
    public TemplatePadrao padrao(EventoNotificacao evento, CanalNotificacao canal) {
        return switch (evento) {
            case ENTRADA_CONFIRMADA -> new TemplatePadrao(
                    "Entrada registrada - {aluno}",
                    "Ola! {aluno} entrou na escola as {hora} do dia {data}. "
                            + "Mensagem automatica, nao responda.");
            case SAIDA_CONFIRMADA -> new TemplatePadrao(
                    "Saida registrada - {aluno}",
                    "Ola! {aluno} saiu da escola as {hora} do dia {data}. "
                            + "Mensagem automatica, nao responda.");
            case TENTATIVA_NAO_AUTORIZADA -> new TemplatePadrao(
                    "Tentativa de retirada nao autorizada - {aluno}",
                    "Atencao: houve uma tentativa NAO autorizada de retirar {aluno} as {hora}. "
                            + "A escola foi alertada. Entre em contato com a secretaria.");
            case RETIRADA_SOLICITADA -> new TemplatePadrao(
                    "Retirada solicitada - {aluno}",
                    "{pessoa} chegou a escola as {hora} para retirar {aluno}. "
                            + "Voce sera avisado quando a saida for confirmada.");
            case JORNADA_PROXIMA_FIM -> new TemplatePadrao(
                    "Jornada perto do fim - {aluno}",
                    "A jornada contratada de {aluno} termina em breve (previsto para {hora}).");
            case HORARIO_EXCEDIDO -> new TemplatePadrao(
                    "Horario excedido - {aluno}",
                    "{aluno} ultrapassou a jornada contratada. Permanencia ate agora: {permanencia}.");
            case EQUIPAMENTO_OFFLINE -> new TemplatePadrao(
                    "Equipamento fora do ar",
                    "O equipamento {equipamento} esta sem comunicacao desde {hora}. "
                            + "Enquanto isso, registre as entradas manualmente.");
            case AUTORIZACAO_APROVADA -> new TemplatePadrao(
                    "Autorizacao aprovada - {aluno}",
                    "A escola aprovou {pessoa} para retirar {aluno}. "
                            + "A autorizacao ja esta valendo.");
        };
    }

    /** Assunto (usado em e-mail) e corpo. */
    public record TemplatePadrao(String assunto, String corpo) {
    }

    /** Lista as variaveis usadas por um template, para a tela de preview. */
    public Map<String, String> extrairMarcadores(String template) {
        Map<String, String> encontrados = new LinkedHashMap<>();
        if (template == null) {
            return encontrados;
        }
        Matcher m = MARCADOR.matcher(template);
        while (m.find()) {
            encontrados.put(m.group(1).toLowerCase(Locale.ROOT), "");
        }
        return encontrados;
    }
}
