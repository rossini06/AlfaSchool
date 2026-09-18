package br.com.alfaschool.backend.domain.access.shared;

/** Ciclo de vida da autorizacao de retirada. Pedido vindo do portal nasce PENDENTE. */
public enum StatusAutorizacao { PENDENTE, ATIVA, SUSPENSA, EXPIRADA, REVOGADA }
