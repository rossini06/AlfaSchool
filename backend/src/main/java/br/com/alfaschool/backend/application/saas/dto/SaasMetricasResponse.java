package br.com.alfaschool.backend.application.saas.dto;

public record SaasMetricasResponse(
    long totalTenants,
    long tenantsAtivos,
    long tenantsEmTrial,
    long totalPlanos
) {}
