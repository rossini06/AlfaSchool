package br.com.alfaschool.backend.domain.access.shared;

/** Eventos que disparam comunicacao. Nunca enviar foto ou dado biometrico de crianca nestas mensagens. */
public enum EventoNotificacao { ENTRADA_CONFIRMADA, SAIDA_CONFIRMADA, RETIRADA_SOLICITADA, JORNADA_PROXIMA_FIM, HORARIO_EXCEDIDO, TENTATIVA_NAO_AUTORIZADA, EQUIPAMENTO_OFFLINE, AUTORIZACAO_APROVADA }
