package br.com.alfaschool.backend.security;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Impede o sistema de subir com os segredos de laboratorio.
 *
 * <h2>Por que isto existe</h2>
 * {@code APP_JWT_SECRET} e {@code APP_SECRET_KEY} tinham valor padrao
 * FUNCIONAL no application.yml. Quem esquecesse de definir as variaveis
 * subia um sistema que funciona perfeitamente — e no qual:
 *
 * <ul>
 *   <li>qualquer pessoa que conheca o repositorio forja um JWT valido, de
 *       qualquer tenant, com qualquer permissao;</li>
 *   <li>a mesma pessoa assina a URL HMAC de foto e baixa o rosto de
 *       qualquer crianca sem nem fazer login;</li>
 *   <li>a senha do superadmin e' a do repositorio.</li>
 * </ul>
 *
 * Nada disso deixa rastro de erro. O sistema nao reclama, porque do ponto
 * de vista dele esta' tudo configurado.
 *
 * <h2>Por que falhar em vez de avisar</h2>
 * Um WARN no log de boot nao e' lido. E o modo de falha aqui nao e' "o
 * sistema para": e' "o sistema funciona e alguem entra". Pela regra 5 do
 * CLAUDE.md — duvida ou erro = negar — a escolha certa e' nao subir.
 *
 * <h2>Como continuar desenvolvendo</h2>
 * {@code APP_PERMITIR_SEGREDOS_PADRAO=true} libera, e o {@code dev.sh} ja'
 * define isso. A permissao e' consciente e aparece no log; o que nao pode
 * e' acontecer por esquecimento.
 */
@Configuration
public class SegredosDeBootGuard {

    private static final Logger log = LoggerFactory.getLogger(SegredosDeBootGuard.class);

    /** Os valores que estao versionados no repositorio. */
    private static final String JWT_DE_LABORATORIO =
            "12345678901234567890123456789012345678901234567890";
    private static final String CIFRA_DE_LABORATORIO =
            "alfaschool-dev-secret-key-trocar-em-producao-32+";
    private static final String SENHA_DE_LABORATORIO = "100%Alfa@";

    private final String jwtSecret;
    private final String secretKey;
    private final String senhaAdmin;
    private final boolean permitido;

    public SegredosDeBootGuard(
            @Value("${app.security.jwt-secret:}") String jwtSecret,
            @Value("${app.secret-key:}") String secretKey,
            @Value("${app.admin.password:}") String senhaAdmin,
            @Value("${app.permitir-segredos-padrao:false}") boolean permitido) {
        this.jwtSecret = jwtSecret;
        this.secretKey = secretKey;
        this.senhaAdmin = senhaAdmin;
        this.permitido = permitido;
    }

    @PostConstruct
    void conferir() {
        List<String> pendencias = new ArrayList<>();
        if (JWT_DE_LABORATORIO.equals(jwtSecret)) {
            pendencias.add("APP_JWT_SECRET (assinatura de token: permite forjar login de qualquer usuário)");
        }
        if (CIFRA_DE_LABORATORIO.equals(secretKey)) {
            pendencias.add("APP_SECRET_KEY (cifra de segredos e assinatura das URLs de foto dos alunos)");
        }
        if (SENHA_DE_LABORATORIO.equals(senhaAdmin)) {
            pendencias.add("APP_SUPERADMIN_PASSWORD (senha do superadministrador)");
        }

        if (pendencias.isEmpty()) {
            return;
        }

        if (permitido) {
            log.warn("=======================================================================");
            log.warn("ATENCAO: rodando com segredos de laboratorio, liberado por");
            log.warn("APP_PERMITIR_SEGREDOS_PADRAO=true. NAO use esta configuracao com");
            log.warn("dado real de aluno. Pendentes: {}", pendencias);
            log.warn("=======================================================================");
            return;
        }

        throw new IllegalStateException(String.join("\n",
                "",
                "=======================================================================",
                " O sistema NAO vai subir com os segredos padrao do repositorio.",
                "",
                " Defina estas variaveis de ambiente com valores proprios:",
                "   - " + String.join("\n   - ", pendencias),
                "",
                " APP_JWT_SECRET e APP_SECRET_KEY precisam de 32+ caracteres. Ex.:",
                "   export APP_JWT_SECRET=$(openssl rand -base64 48)",
                "   export APP_SECRET_KEY=$(openssl rand -base64 48)",
                "",
                " Em desenvolvimento, para seguir com os valores de laboratorio:",
                "   export APP_PERMITIR_SEGREDOS_PADRAO=true",
                "",
                " Trocar APP_SECRET_KEY depois de gravar dado cifrado torna ilegivel",
                " a senha de equipamento e a credencial de provedor ja' salvas — defina",
                " a chave ANTES de cadastrar equipamento.",
                "======================================================================="));
    }
}
