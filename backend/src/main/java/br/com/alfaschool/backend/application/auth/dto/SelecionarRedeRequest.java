package br.com.alfaschool.backend.application.auth.dto;

import java.util.UUID;

/** Nulo = voltar ao proprio tenant (o mestre, no caso do superadministrador). */
public record SelecionarRedeRequest(UUID tenantId) {
}
