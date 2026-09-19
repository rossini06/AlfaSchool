package br.com.alfaschool.backend.shared.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Traduz os parametros de pagina da requisicao em {@link Pageable}.
 *
 * <h2>Por que nao chamar PageRequest.of direto no controller</h2>
 * Porque o valor vem do cliente. {@code PageRequest.of} lanca
 * {@code IllegalArgumentException} para pagina negativa ou tamanho zero, e
 * essa excecao nao tem tratamento proprio: caia no catch-all e virava
 * <b>500</b>. Um {@code ?page=-1} digitado na barra de endereco derrubava
 * TODA listagem paginada do sistema com "erro interno" — 13 endpoints
 * verificados.
 *
 * <h2>Por que ajustar em vez de recusar</h2>
 * Pagina e tamanho nao sao dado de negocio: sao navegacao. Recusar com 400
 * faz a tela quebrar por causa de um parametro que o proprio front montou
 * errado. Ajustar para o limite mais proximo mantem a lista no ar, e o
 * numero devolvido na resposta mostra o que foi usado.
 *
 * <h2>O teto</h2>
 * {@code ?size=2000000000} respondia 200 e mandava o banco materializar a
 * tabela inteira em memoria. Numa escola pequena passa despercebido; numa
 * rede com dezenas de milhares de alunos e' derrubar a API com uma URL.
 */
public final class Paginacao {

    /** Acima disto nao e' mais tela, e' exportacao — que tem endpoint proprio. */
    public static final int TAMANHO_MAXIMO = 200;
    public static final int TAMANHO_PADRAO = 20;

    private Paginacao() {
    }

    public static Pageable de(int page, int size) {
        return PageRequest.of(pagina(page), tamanho(size));
    }

    public static Pageable de(int page, int size, Sort sort) {
        return PageRequest.of(pagina(page), tamanho(size), sort);
    }

    static int pagina(int page) {
        return Math.max(page, 0);
    }

    static int tamanho(int size) {
        if (size <= 0) {
            return TAMANHO_PADRAO;
        }
        return Math.min(size, TAMANHO_MAXIMO);
    }
}
