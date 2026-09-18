package br.com.alfaschool.backend.domain.access.shared;

/** Veredito da foto POR equipamento. Sem isto a tela mente dizendo sincronizado quando o leitor recusou a face. */
public enum StatusFaceSync { PENDENTE, ENVIADA, ACEITA, RECUSADA, REMOVIDA }
