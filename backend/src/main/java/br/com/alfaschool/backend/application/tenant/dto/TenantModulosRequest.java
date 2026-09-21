package br.com.alfaschool.backend.application.tenant.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Conjunto completo: o que nao estiver aqui deixa de valer. */
public record TenantModulosRequest(@NotNull List<String> modulos) {
}
