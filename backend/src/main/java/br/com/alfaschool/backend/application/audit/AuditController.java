package br.com.alfaschool.backend.application.audit;

import br.com.alfaschool.backend.application.audit.dto.AuditLogResponse;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AuditLogRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import br.com.alfaschool.backend.shared.web.Paginacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auditoria")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    public AuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_AUDITORIA_VER')")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> list(
            @RequestParam(required = false) String modulo,
            @RequestParam(required = false) String acao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant não identificado");
        }

        Pageable pageable = Paginacao.de(page, size, Sort.by("timestamp").descending());

        Instant from = dateFrom != null ? dateFrom.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Instant to = dateTo != null ? dateTo.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC) : null;

        Page<AuditLogResponse> result = auditLogRepository
                .findFiltered(tenantId, acao, modulo, from, to, pageable)
                .map(AuditLogResponse::from);

        return ResponseEntity.ok(ApiResponse.of(200, "Logs de auditoria listados com sucesso", result));
    }
}
