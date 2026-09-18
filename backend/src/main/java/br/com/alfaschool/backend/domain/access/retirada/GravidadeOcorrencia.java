package br.com.alfaschool.backend.domain.access.retirada;

/**
 * Quanto a ocorrencia corre. CRITICA e' o que nao espera o dia seguinte:
 * restricao judicial violada, pessoa desconhecida tentando retirar crianca.
 *
 * Nao existe em domain/access/shared (nao havia enum de gravidade la');
 * fica aqui, junto de AccOcorrencia, que e' quem usa.
 */
public enum GravidadeOcorrencia {
    BAIXA, MEDIA, ALTA, CRITICA
}
