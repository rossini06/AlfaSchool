package br.com.alfaschool.backend.application.dispositivo;

import br.com.alfaschool.backend.application.dispositivo.dto.DispositivoRequest;
import br.com.alfaschool.backend.application.dispositivo.dto.DispositivoResponse;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dispositivos")
public class DispositivoController {

    private final DispositivoService dispositivoService;

    public DispositivoController(DispositivoService dispositivoService) {
        this.dispositivoService = dispositivoService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<DispositivoResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("nome").ascending());
        return ResponseEntity.ok(ApiResponse.of(200, "Dispositivos listados com sucesso", dispositivoService.list(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DispositivoResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Dispositivo encontrado", dispositivoService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DispositivoResponse>> create(@Valid @RequestBody DispositivoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Dispositivo criado com sucesso", dispositivoService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DispositivoResponse>> update(@PathVariable UUID id,
                                                                    @Valid @RequestBody DispositivoRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Dispositivo atualizado com sucesso", dispositivoService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        dispositivoService.delete(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Dispositivo removido com sucesso", null));
    }

    @PostMapping("/{id}/ping")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DispositivoResponse>> ping(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Ping registrado com sucesso", dispositivoService.ping(id)));
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DispositivoResponse>> toggle(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Status do dispositivo alterado com sucesso", dispositivoService.toggle(id)));
    }
}
