package br.com.alfaschool.backend.domain.access.shared;

/** Maquina de estados da retirada. A transicao ENTREGUE nao encerra a permanencia; so a saida efetiva encerra. */
public enum StatusRetirada { SOLICITADA, PREPARANDO, PRONTO, ENTREGUE, CANCELADA, NEGADA }
