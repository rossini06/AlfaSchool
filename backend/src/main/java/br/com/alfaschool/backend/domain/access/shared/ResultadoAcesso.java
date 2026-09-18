package br.com.alfaschool.backend.domain.access.shared;

/** Veredito do equipamento na leitura. NEGADO e DESCONHECIDO tambem sao gravados: o relatorio de tentativas depende deles. */
public enum ResultadoAcesso { PERMITIDO, NEGADO, DESCONHECIDO }
