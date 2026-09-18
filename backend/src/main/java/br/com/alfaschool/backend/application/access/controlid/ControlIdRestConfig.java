package br.com.alfaschool.backend.application.access.controlid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * UM unico RestClient para falar com todos os leitores.
 *
 * Os projetos anteriores abriam HttpURLConnection na mao em cada metodo:
 * seis copias da mesma configuracao, nenhuma com pool, cada uma com o
 * proprio timeout (ou sem timeout nenhum). Um leitor inalcancavel travava
 * a thread ate' o SO desistir.
 *
 * Timeouts curtos de proposito. O equipamento esta na LAN da escola: se
 * nao respondeu em 5 segundos, esta offline, e insistir so' segura a
 * requisicao da secretaria. O HttpClient do JDK mantem pool de conexoes
 * keep-alive por host, o que importa num full sync de 2000 alunos.
 */
@Configuration
public class ControlIdRestConfig {

    @Bean("controlIdRestClient")
    public RestClient controlIdRestClient(
            @Value("${app.access.controlid.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${app.access.controlid.read-timeout-ms:8000}") int readTimeoutMs) {

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                // O firmware nao faz HTTPS nem HTTP/2; forcar 1.1 evita o
                // upgrade negociado que alguns modelos respondem com reset.
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder()
                .requestFactory(factory)
                // Erro HTTP nao vira excecao aqui: a recusa de foto chega
                // como 200 com success=false e o "invalid command" como
                // 400 com corpo util. Quem interpreta e' o ControlIdClient.
                .defaultStatusHandler(status -> true, (req, res) -> { })
                .build();
    }
}
